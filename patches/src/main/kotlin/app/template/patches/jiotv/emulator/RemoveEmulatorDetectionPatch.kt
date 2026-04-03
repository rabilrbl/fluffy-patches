package app.template.patches.jiotv.emulator

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

val removeEmulatorDetectionPatch = bytecodePatch(
    name = "Remove emulator detection",
    description = "Removes emulator and unsupported device detection checks in PermissionActivity. Bypasses Build.FINGERPRINT/MODEL/BRAND/HARDWARE checks, BlueStacks folder detection, Fire TV detection, and Android TV (leanback) feature checks.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        mutableClassDefBy("Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;")
            .virtualMethods
            .first { it.name == "isRunningOnEmulator" }
            .addInstructions(0, "const/4 v0, 0x0\nreturn v0")

        mutableClassDefBy("Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;")
            .virtualMethods
            .first { it.name == "isSupportedDevice" }
            .addInstructions(0, "const/4 v0, 0x1\nreturn v0")

        mutableClassDefBy("Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;")
            .virtualMethods
            .first { it.name == "onCreate" }
            .addInstructions(0, "invoke-super {p0}, Landroidx/appcompat/app/AppCompatActivity;->onCreate(Landroid/os/Bundle;)V\ninvoke-virtual {p0}, Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;->proceedApplication()V\nreturn-void")
    }
}
