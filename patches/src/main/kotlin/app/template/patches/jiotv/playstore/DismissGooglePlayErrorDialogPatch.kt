package app.template.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val dismissGooglePlayErrorDialogPatch = bytecodePatch(
    name = "Dismiss Google Play error dialog",
    description = "Prevents the 'Something went wrong - Check that Google Play is enabled' dialog from appearing when license check fails on devices without Play Store.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        classDefBy("Lcom/pairip/licensecheck3/LicenseClientV3;")
            .methods.first { it.name == "handleError" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}
