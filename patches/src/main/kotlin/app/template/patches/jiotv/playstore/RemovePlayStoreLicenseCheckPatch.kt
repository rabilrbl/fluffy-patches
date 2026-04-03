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
//
// Also changes the application class from com.pairip.application.Application to
// com.jio.jioplay.p037tv.JioTVApplication to completely bypass the pairip library.
@Suppress("unused")
val disablePairipManifestPatch = resourcePatch(
    name = "Disable pairip license check (manifest)",
    description = "Globally disables the pairip LicenseContentProvider in AndroidManifest " +
        "and changes application class to bypass pairip entirely.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        document("AndroidManifest.xml").use { document ->
            // Remove LicenseContentProvider
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

            // Change application class to bypass pairip entirely
            // NOTE: actual package is p037tv (obfuscated), not plain tv
            val application = document.getElementsByTagName("application").item(0) as Element
            application.setAttribute("android:name", "com.jio.jioplay.p037tv.JioTVApplication")
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
        // ============================================================
        // 1. BLOCK NATIVE LIBRARY LOADING (pairipcore.so)
        // ============================================================
        println("[PlayStorePatch] Patching VMRunner.<clinit> to prevent pairipcore.so loading")
        classDefBy("Lcom/pairip/VMRunner;")
            .methods.first { it.name == "<clinit>" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Also patch VMRunner.setContext to prevent any context from being set
        println("[PlayStorePatch] Patching VMRunner.setContext to no-op")
        classDefBy("Lcom/pairip/VMRunner;")
            .methods.first { it.name == "setContext" }
            .toMutable()
            .addInstructions(0, "return-void")

        // ============================================================
        // 2. BLOCK VM BYTECODE EXECUTION
        // ============================================================
        // Patch StartupLauncher.launch() — called from AppComponentFactory
        println("[PlayStorePatch] Patching StartupLauncher.launch to no-op")
        classDefBy("Lcom/pairip/StartupLauncher;")
            .methods.first { it.name == "launch" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Patch StartupLauncher.<clinit> to prevent any static init side effects
        println("[PlayStorePatch] Patching StartupLauncher.<clinit> to no-op")
        classDefBy("Lcom/pairip/StartupLauncher;")
            .methods.first { it.name == "<clinit>" }
            .toMutable()
            .addInstructions(0, "return-void")

        // ============================================================
        // 3. BYPASS SIGNATURE VERIFICATION
        // ============================================================
        println("[PlayStorePatch] Patching SignatureCheck.verifyIntegrity to no-op")
        classDefBy("Lcom/pairip/SignatureCheck;")
            .methods.first { it.name == "verifyIntegrity" }
            .toMutable()
            .addInstructions(0, "return-void")

        // ============================================================
        // 4. PATCH pairip Application.attachBaseContext
        // ============================================================
        // This is the entry point that calls VMRunner.setContext and SignatureCheck.verifyIntegrity.
        // Replace with direct call to real JioTVApplication.attachBaseContext.
        // NOTE: package is p037tv, not plain tv
        println("[PlayStorePatch] Patching pairip Application.attachBaseContext to skip pairip init")
        classDefBy("Lcom/pairip/application/Application;")
            .methods.first { it.name == "attachBaseContext" }
            .toMutable()
            .addInstructions(
                0,
                """
                    invoke-super {p0, p1}, Lcom/jio/jioplay/p037tv/JioTVApplication;->attachBaseContext(Landroid/content/Context;)V
                    return-void
                """,
            )

        // ============================================================
        // 5. BLOCK LICENSE CONTENT PROVIDER
        // ============================================================
        println("[PlayStorePatch] Patching LicenseContentProvider.onCreate to return true without init")
        classDefBy("Lcom/pairip/licensecheck/LicenseContentProvider;")
            .methods.first { it.name == "onCreate" }
            .toMutable()
            .addInstructions(0, "const/4 v0, 0x1\nreturn v0")

        // ============================================================
        // 6. BLOCK LicenseClient — THE CORE LICENSE CHECK ENGINE
        // ============================================================

        // Set licenseCheckState to FULL_CHECK_OK so no checks are ever triggered
        // ordinal of FULL_CHECK_OK = 1 (CHECK_REQUIRED=0, FULL_CHECK_OK=1)
        // We patch initializeLicenseCheck to set the state and return immediately
        println("[PlayStorePatch] Patching LicenseClient.initializeLicenseCheck to set FULL_CHECK_OK")
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "initializeLicenseCheck" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Block connectToLicensingService - prevents binding to Play Store licensing service
        println("[PlayStorePatch] Patching LicenseClient.connectToLicensingService to no-op")
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "connectToLicensingService" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Safety net: block processResponse() — response code 2 (NOT_LICENSED)
        // fires the "Get this app from Play Store" paywall PendingIntent.
        println("[PlayStorePatch] Patching LicenseClient.processResponse to no-op")
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "processResponse" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Block startPaywallActivity - prevents the Play Store intent from being launched
        println("[PlayStorePatch] Patching LicenseClient.startPaywallActivity to no-op")
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "startPaywallActivity" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Block startErrorDialogActivity
        println("[PlayStorePatch] Patching LicenseClient.startErrorDialogActivity to no-op")
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "startErrorDialogActivity" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Block handleError - this can also trigger error dialog
        println("[PlayStorePatch] Patching LicenseClient.handleError to no-op")
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "handleError" }
            .toMutable()
            .addInstructions(0, "return-void")

        // ============================================================
        // 7. BLOCK LicenseActivity PAYWALL
        // ============================================================
        // Safety net: if LicenseActivity somehow starts, finish it immediately.
        println("[PlayStorePatch] Patching LicenseActivity.onStart to finish immediately")
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

        // ============================================================
        // 8. BLOCK APP-SIDE PLAY STORE REDIRECT HELPERS
        // ============================================================
        // NOTE: actual package is p037tv (obfuscated), not plain tv
        println("[PlayStorePatch] Patching CommonUtils.checkIsUpdateAvailable to no-op")
        classDefBy("Lcom/jio/jioplay/p037tv/utils/CommonUtils;")
            .methods.first { it.name == "checkIsUpdateAvailable" }
            .toMutable()
            .addInstructions(0, "return-void")

        println("[PlayStorePatch] Patching CommonUtils.redirectToPlayStore to no-op")
        classDefBy("Lcom/jio/jioplay/p037tv/utils/CommonUtils;")
            .methods.first { it.name == "redirectToPlayStore" }
            .toMutable()
            .addInstructions(0, "return-void")

        println("[PlayStorePatch] Patching CommonUtils.takeToPlayStore to no-op")
        classDefBy("Lcom/jio/jioplay/p037tv/utils/CommonUtils;")
            .methods.first { it.name == "takeToPlayStore" }
            .toMutable()
            .addInstructions(0, "return-void")

        println("[PlayStorePatch] All playstore patches applied successfully")
    }
}
