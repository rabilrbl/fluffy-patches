package app.rabil.patches.jiotv.emulator

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val removeEmulatorDetectionPatch = bytecodePatch(
    name = "Remove emulator detection",
    description = "Removes emulator and unsupported device detection checks in PermissionActivity. " +
        "Bypasses Build.FINGERPRINT/MODEL/BRAND/HARDWARE checks, BlueStacks folder detection, " +
        "Fire TV detection, and Android TV (leanback) feature checks.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        // --- PermissionActivity.isRunningOnEmulator() → always return false ---
        // Checks:
        //   Build.FINGERPRINT starts with "generic" or "unknown"
        //   Build.MODEL contains "google_sdk", "Emulator", "Android SDK built for x86"
        //   Build.MANUFACTURER contains "Genymotion"
        //   Build.BRAND + DEVICE start with "generic"
        //   Build.PRODUCT equals "google_sdk" or contains "vbox86p"
        //   Build.DEVICE contains "vbox86p"
        //   Build.HARDWARE contains "vbox86"
        // Also in onCreate: checks for /sdcard/windows/BstSharedFolder (BlueStacks)
        // and android.software.leanback system feature (Android TV).
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

        // --- PermissionActivity.isSupportedDevice() → always return true ---
        // Checks for amazon.hardware.fire_tv system feature and
        // Build.MODEL containing "AFT" (Amazon Fire TV stick/cube).
        // Returning true ensures the app runs on Fire TV and all device types.
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
