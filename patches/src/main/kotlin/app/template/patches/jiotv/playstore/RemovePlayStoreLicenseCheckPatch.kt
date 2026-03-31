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
        // --- Bypass LicenseClient.initializeLicenseCheck() ---
        // Main entry point for pairip's Play Store license verification.
        // Connects to com.android.vending licensing service and validates the purchase.
        // Making it return-void immediately prevents all license checks.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "initializeLicenseCheck" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Bypass connectToLicensingService() ---
        // Prevents binding to com.android.vending licensing service.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "connectToLicensingService" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Bypass performLocalInstallerCheck() → always return true ---
        // Checks getInstallSourceInfo() for "com.android.vending" as the installer.
        // For sideloaded APKs this fails. We make it always return true.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "performLocalInstallerCheck" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )

        // --- Neuter LicenseActivity.onStart() ---
        // Shows error dialogs and paywall intents when license check fails.
        // Preventing it from executing ensures no blocking UI is shown.
        classDefBy("Lcom/pairip/licensecheck/LicenseActivity;")
            .methods.first { it.name == "onStart" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Hard-disable popup rendering paths in LicenseActivity ---
        // Some builds can still route into these private UI methods.
        classDefBy("Lcom/pairip/licensecheck/LicenseActivity;")
            .methods.first { it.name == "showPaywallAndCloseApp" }
            .toMutable()
            .addInstructions(0, "return-void")

        classDefBy("Lcom/pairip/licensecheck/LicenseActivity;")
            .methods.first { it.name == "showErrorDialog" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Prevent forced process termination if LicenseActivity is ever launched.
        classDefBy("Lcom/pairip/licensecheck/LicenseActivity;")
            .methods.first { it.name == "closeApp" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Block popup launchers in LicenseClient ---
        // These two methods are responsible for opening blocking UI.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "startErrorDialogActivity" }
            .toMutable()
            .addInstructions(0, "return-void")

        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "startPaywallActivity" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Fail-safe on response/error handlers ---
        // If the service still answers with NOT_LICENSED, suppress downstream handling.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "processResponse" }
            .toMutable()
            .addInstructions(0, "return-void")

        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "handleError" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Disable content provider bootstrap ---
        // LicenseContentProvider.onCreate() eagerly triggers license init on app startup.
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
    }
}
