package app.template.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

val disablePairipManifestPatch = resourcePatch(
    name = "Disable pairip license check (manifest)",
    description = "Removes the pairip LicenseContentProvider from AndroidManifest to prevent auto-initialization of license checking.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        document("AndroidManifest.xml").use { doc ->
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
    description = "Bypasses pairip APK signature verification, license checking, paywall, and Play Store redirect helpers. The VM execution itself is left intact since the app's code is encrypted in assets/.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        mutableClassDefBy("Lcom/pairip/SignatureCheck;")
            .directMethods
            .first { it.name == "verifyIntegrity" }
            .addInstructions(0, "return-void")

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
            .first { it.name == "getCheckAppUpadteData" }
            .addInstructions(0, "const/4 v0, 0x0\nreturn v0")

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

        mutableClassDefBy("Lcom/jio/jioplay/tv/utils/AppUpdateHelper;")
            .virtualMethods
            .first { it.name == "checkUpdate" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/jio/jioplay/tv/utils/AppUpdateHelper;")
            .virtualMethods
            .first { it.name == "checkUpdatefordiag" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/jio/jioplay/tv/utils/AppUpdateHelper;")
            .virtualMethods
            .first { it.name == "resumeUpdate" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/jio/jioplay/tv/utils/AppUpdateHelper;")
            .directMethods
            .first { it.name == "a" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/jio/jioplay/tv/utils/AppUpdateHelper;")
            .directMethods
            .first { it.name == "<init>" }
            .addInstructions(0, "return-void")

        mutableClassDefBy("Lcom/jio/jioplay/tv/activities/HomeActivity;")
            .virtualMethods
            .first { it.name == "onCreate" }
            .addInstructions(0, "const/4 v0, 0x0\ninvoke-static {v0}, Lcom/jio/jioplay/tv/utils/CommonUtils;->setCheckAppUpadteData(Lcom/jio/jioplay/tv/data/network/response/CheckAppUpadteData;)V")

        mutableClassDefBy("Lcom/jio/jioplay/tv/activities/HomeActivity;")
            .virtualMethods
            .first { it.name == "onResume" }
            .addInstructions(0, "invoke-super {p0}, Landroidx/appcompat/app/AppCompatActivity;->onResume()V\nreturn-void")
    }
}
