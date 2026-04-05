package app.template.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
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

@Suppress("unused")
val removePlayStoreLicenseCheckPatch = bytecodePatch(
    name = "Remove Play Store license check",
    description = "Removes the Play Store installation and license verification check (pairip).",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "initializeLicenseCheck" }
            .toMutable()
            .addInstructions(0, "return-void")

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

        classDefBy("Lcom/pairip/licensecheck/LicenseActivity;")
            .methods.first { it.name == "onStart" }
            .toMutable()
            .addInstructions(0, "return-void")

        classDefBy("Lcom/pairip/SignatureCheck;")
            .methods.first { it.name == "verifyIntegrity" }
            .toMutable()
            .addInstructions(0, "return-void")

        classDefBy("Lcom/pairip/StartupLauncher;")
            .methods.first { it.name == "launch" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}
