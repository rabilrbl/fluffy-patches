package app.template.patches.jiotv.emulator

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val removeEmulatorDetectionPatch = bytecodePatch(
    name = "Remove emulator detection",
    description = "Removes emulator and unsupported device detection checks in PermissionActivity.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        classDefBy("Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;")
            .methods.first { it.name == "isRunningOnEmulator" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )

        classDefBy("Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;")
            .methods.first { it.name == "isSupportedDevice" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )
    }
}
