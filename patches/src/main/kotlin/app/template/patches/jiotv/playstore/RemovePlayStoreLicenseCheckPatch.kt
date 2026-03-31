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
            for (i in 0 until providers.length) {
                val provider = providers.item(i) as Element
                if (provider.getAttribute("android:name")
                        .contains("LicenseContentProvider")) {
                    provider.setAttribute("android:enabled", "false")
                }
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
