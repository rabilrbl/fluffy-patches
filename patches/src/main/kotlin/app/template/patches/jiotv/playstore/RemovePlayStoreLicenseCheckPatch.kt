package app.template.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

val disablePairipManifestPatch = resourcePatch(
    name = "Disable pairip license check (manifest)",
    description = "Globally disables the pairip LicenseContentProvider in AndroidManifest and changes application class to bypass pairip entirely.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        document("AndroidManifest.xml").use { doc ->
            val appElement = doc.getElementsByTagName("application").item(0) as org.w3c.dom.Element
            appElement.setAttribute("android:name", "com.jio.jioplay.tv.JioTVApplication")

            val providers = doc.getElementsByTagName("provider")
            val toRemove = mutableListOf<org.w3c.dom.Element>()
            for (i in 0 until providers.length) {
                val provider = providers.item(i) as org.w3c.dom.Element
                val name = provider.getAttribute("android:name")
                if (name == "com.pairip.licensecheck.LicenseContentProvider") {
                    toRemove.add(provider)
                }
            }
            for (el in toRemove) {
                el.parentNode.removeChild(el)
            }
        }
    }
}

val removePlayStoreLicenseCheckPatch = bytecodePatch(
    name = "Remove Play Store license check",
    description = "Neutralizes pairip native library loading, signature verification, VM bytecode execution, LicenseActivity paywall, and Play Store redirect helpers.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    dependsOn(disablePairipManifestPatch)

    execute {
        mutableClassDefBy("Lcom/pairip/VMRunner;")
            .directMethods
            .first { it.name == "<clinit>" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/pairip/VMRunner;")
            .directMethods
            .first { it.name == "setContext" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/pairip/StartupLauncher;")
            .directMethods
            .first { it.name == "launch" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/pairip/SignatureCheck;")
            .directMethods
            .first { it.name == "verifyIntegrity" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/pairip/application/Application;")
            .virtualMethods
            .first { it.name == "attachBaseContext" }
            .addInstructions(0, "invoke-super {p0, p1}, Lcom/jio/jioplay/tv/JioTVApplication;->attachBaseContext(Landroid/content/Context;)V\nreturn-void")

        mutableClassDefBy("Lcom/pairip/licensecheck/LicenseContentProvider;")
            .virtualMethods
            .first { it.name == "onCreate" }
            .addInstructions(0, "const/4 v0, 0x1\nreturn v0")

        mutableClassDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .virtualMethods
            .first { it.name == "initializeLicenseCheck" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .directMethods
            .first { it.name == "connectToLicensingService" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .directMethods
            .first { it.name == "processResponse" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .directMethods
            .first { it.name == "startPaywallActivity" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .directMethods
            .first { it.name == "startErrorDialogActivity" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .directMethods
            .first { it.name == "handleError" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/pairip/licensecheck/LicenseActivity;")
            .virtualMethods
            .first { it.name == "onStart" }
            .addInstructions(0, "invoke-super {p0}, Landroid/app/Activity;->onStart()V\ninvoke-virtual {p0}, Lcom/pairip/licensecheck/LicenseActivity;->finish()V\nreturn-void")

        mutableClassDefBy("Lcom/jio/jioplay/tv/utils/CommonUtils;")
            .directMethods
            .first { it.name == "checkIsUpdateAvailable" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/jio/jioplay/tv/utils/CommonUtils;")
            .directMethods
            .first { it.name == "redirectToPlayStore" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/jio/jioplay/tv/utils/CommonUtils;")
            .directMethods
            .first { it.name == "takeToPlayStore" }
            .addInstructions(0, "return-void")
    }
}
