package app.rabil.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE
import org.w3c.dom.Element

// --- Resource patch: global manifest-level disable of pairip ---
// LicenseContentProvider is the sole Java-side bootstrap for all pairip license
// checks. Setting android:enabled="false" in the manifest is the cleanest global
// disable — Android never calls its onCreate(), so the entire check flow never
// starts regardless of app version or code changes.
@Suppress("unused")
val disablePairipManifestPatch = resourcePatch(
    name = "Disable pairip license check (manifest)",
    description = "Globally disables the pairip LicenseContentProvider in AndroidManifest " +
        "so Android never initializes it, preventing all Play Store license checks.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        document("AndroidManifest.xml").use { document ->
            val providers = document.getElementsByTagName("provider")
            val toRemove = mutableListOf<Element>()
            for (i in 0 until providers.length) {
                val provider = providers.item(i) as Element
                if (provider.getAttribute("android:name")
                        .contains("LicenseContentProvider")) {
                    toRemove.add(provider)
                }
            }
            toRemove.forEach { provider ->
                provider.parentNode?.removeChild(provider)
            }
        }
    }
}

// --- Bytecode patch: neutralize native library and residual pairip paths ---
// The manifest patch handles the Java path. These patches block the native path:
// pairipcore.so is loaded via VMRunner.<clinit> early in app startup (before
// the ContentProvider even runs), and its JNI_OnLoad() can independently fire
// a Play Store intent. LicenseActivity and processResponse() are patched as
// final safety nets in case anything still triggers the paywall flow.
@Suppress("unused")
val removePlayStoreLicenseCheckPatch = bytecodePatch(
    name = "Remove Play Store license check",
    description = "Neutralizes pairip native library loading, signature verification, " +
        "VM bytecode execution, LicenseActivity paywall, and Play Store redirect helpers.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        // Prevent pairipcore.so from loading — blocks all JNI-level checks.
        classDefBy("Lcom/pairip/VMRunner;")
            .methods.first { it.name == "<clinit>" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Prevent VM bytecode execution via StartupLauncher.
        classDefBy("Lcom/pairip/StartupLauncher;")
            .methods.first { it.name == "launch" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Bypass signature verification to avoid SignatureTamperedException.
        classDefBy("Lcom/pairip/SignatureCheck;")
            .methods.first { it.name == "verifyIntegrity" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Safety net: if LicenseActivity somehow starts, finish it immediately.
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

        // Safety net: block processResponse() — response code 2 (NOT_LICENSED)
        // fires the "Get this app from Play Store" paywall PendingIntent.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "processResponse" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Block initializeLicenseCheck() - this is called by LicenseContentProvider
        // and can trigger the licensing service connection even after processResponse
        // is patched. This prevents the entire license check flow from starting.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "initializeLicenseCheck" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Force local installer check to always return true - this bypasses the check
        // that fails when app is installed from unknown source (non-Play Store).
        // The method returns false for non-Play Store installs, triggering the full
        // licensing check which shows the Play Store intent.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "performLocalInstallerCheck" }
            .toMutable()
            .addInstructions(0, "const/4 v0, 0x1\nreturn v0")

        // Block connectToLicensingService - prevents binding to Play Store licensing service
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "connectToLicensingService" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Block startPaywallActivity - prevents the Play Store intent from being launched
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "startPaywallActivity" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Block startErrorDialogActivity
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "startErrorDialogActivity" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Block LicenseContentProvider.onCreate to prevent it from initializing LicenseClient
        classDefBy("Lcom/pairip/licensecheck/LicenseContentProvider;")
            .methods.first { it.name == "onCreate" }
            .toMutable()
            .addInstructions(0, "const/4 v0, 0x1\nreturn v0")

        // Disable app-side Play Store redirect helpers.
        classDefBy("Lcom/jio/jioplay/tv/utils/CommonUtils;")
            .methods.first { it.name == "checkIsUpdateAvailable" }
            .toMutable()
            .addInstructions(0, "return-void")

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
