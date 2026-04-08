package app.template.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val bypassPairipPatch = bytecodePatch(
    name = "Dismiss Google Play error dialog (371 split research)",
    description = "Experimental JioTV 7.1.7 split-build patch. Suppresses LicenseClientV3 error handling only and does not disable VMRunner or StartupLauncher.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        classDefBy("Lcom/pairip/licensecheck3/LicenseClientV3;")
            .methods.first { it.name == "handleError" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}
