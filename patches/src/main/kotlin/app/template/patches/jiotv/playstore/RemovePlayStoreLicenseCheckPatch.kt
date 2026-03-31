package app.rabil.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val removePlayStoreLicenseCheckPatch = bytecodePatch(
    name = "Remove Play Store license check",
    description = "Removes Play Store/license enforcement (pairip + app-side updater redirects). " +
        "Bypasses LicenseClient, SignatureCheck, StartupLauncher, and CommonUtils store/update gates.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        // --- Disable content provider bootstrap ---
        // LicenseContentProvider.onCreate() is the sole Java-side entry point that
        // triggers license checking. It calls LicenseClient.initializeLicenseCheck()
        // which binds to com.android.vending and initiates the full check flow.
        // This is the primary Java-layer block.
        classDefBy("Lcom/pairip/licensecheck/LicenseContentProvider;")
            .methods.first { it.name == "onCreate" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )

        // --- Bypass LicenseClient.initializeLicenseCheck() ---
        // Defense-in-depth: stops the license check even if something other than
        // LicenseContentProvider manages to instantiate a LicenseClient and call this.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "initializeLicenseCheck" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Block service connection and response processing ---
        // Defense-in-depth: even if initializeLicenseCheck() somehow runs, the
        // actual Play Store connection and response handling are also blocked.
        // processResponse() is where the paywall PendingIntent is fired when
        // the service returns NOT_LICENSED (code 2) — this is the critical block.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "connectToLicensingService" }
            .toMutable()
            .addInstructions(0, "return-void")

        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "processResponse" }
            .toMutable()
            .addInstructions(0, "return-void")

        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "handleError" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Neuter LicenseActivity as the final UI safety net ---
        // If LicenseActivity is somehow started despite the above blocks, finish()
        // immediately dismisses it before onStart() can fire the paywall PendingIntent
        // or show the error dialog. closeApp() is also neutered to prevent System.exit().
        classDefBy("Lcom/pairip/licensecheck/LicenseActivity;")
            .methods.first { it.name == "onStart" }
            .toMutable()
            .addInstructions(
                0,
                """
                    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                    return-void
                """,
            )

        classDefBy("Lcom/pairip/licensecheck/LicenseActivity;")
            .methods.first { it.name == "closeApp" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Bypass SignatureCheck.verifyIntegrity() ---
        // Verifies APK signature hash against hardcoded SHA-256 values:
        //   expectedSignature = "VkwE0TgslZMpxvR+ldSXr9FRIQ5NlCaBT+tvpXr3rTA="
        //   ALLOWLISTED_SIG   = "Vn3kj4pUblROi2S+QfRRL9nhsaO2uoHQg6+dpEtxdTE="
        // Patched/re-signed APKs have different signatures → SignatureTamperedException.
        classDefBy("Lcom/pairip/SignatureCheck;")
            .methods.first { it.name == "verifyIntegrity" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Prevent pairipcore native library from loading ---
        // VMRunner.<clinit> calls System.loadLibrary("pairipcore"), which loads the
        // native integrity-check library. This happens the moment VMRunner is first
        // referenced — which occurs in Application.attachBaseContext() via
        // VMRunner.setContext(), before any other pairip Java-side code runs.
        // The native JNI_OnLoad() can perform its own APK signature check and fire
        // a Play Store Activity intent at the JNI level, bypassing all Java patches.
        // Patching <clinit> to return-void prevents the library from ever loading.
        classDefBy("Lcom/pairip/VMRunner;")
            .methods.first { it.name == "<clinit>" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Bypass StartupLauncher.launch() ---
        // Loads encrypted VM bytecode (file "mVBwD2didVTgj5k7") from assets/ and
        // executes it via native libpairipcore.so through VMRunner.invoke().
        // The VM bytecode performs additional integrity checks at runtime.
        classDefBy("Lcom/pairip/StartupLauncher;")
            .methods.first { it.name == "launch" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Disable app-side update trigger path ---
        // PermissionActivity.onCreate() calls CommonUtils.checkIsUpdateAvailable() on startup.
        // Returning early suppresses server-driven forced update/store popup flow.
        classDefBy("Lcom/jio/jioplay/tv/utils/CommonUtils;")
            .methods.first { it.name == "checkIsUpdateAvailable" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Disable Play Store redirection helpers used by in-app dialogs ---
        classDefBy("Lcom/jio/jioplay/tv/utils/CommonUtils;")
            .methods.first { it.name == "redirectToPlayStore" }
            .toMutable()
            .addInstructions(0, "return-void")

        classDefBy("Lcom/jio/jioplay/tv/utils/CommonUtils;")
            .methods.first { it.name == "takeToPlayStore" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Spoof InstallReferrerClient as ready and from Play Store ---
        // isReady() returning true prevents callers from re-initiating a connection.
        // startConnection() immediately fires the success callback (code 0 = OK) so
        // any analytics flow that reads the install referrer proceeds without blocking.
        classDefBy("Lcom/android/installreferrer/api/b;")
            .methods.first { it.name == "isReady" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )

        classDefBy("Lcom/android/installreferrer/api/b;")
            .methods.first { it.name == "startConnection" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    invoke-interface {p1, v0}, Lcom/android/installreferrer/api/InstallReferrerStateListener;->onInstallReferrerSetupFinished(I)V
                    return-void
                """,
            )
    }
}
