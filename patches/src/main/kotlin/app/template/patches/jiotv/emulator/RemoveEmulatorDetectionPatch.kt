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

        // --- Force proceedApplication to be called in onCreate ---
        // The original onCreate has a complex condition that checks:
        //   isSupportedDevice && isValidBuild && isValidVersionName && !isRooted && 
        //   !BlueStacks && !isEmulator && !leanbackFeature
        //
        // Since the leanback check uses getPackageManager().hasSystemFeature() which is a
        // system call that returns different values based on device type, we patch the 
        // onCreate method to ALWAYS proceed to proceedApplication() regardless of checks.
        //
        // This patch replaces the condition check logic:
        // Original: if (allChecksPass) { proceedApplication(); } else { showErrorDialog(); }
        // Patched: Always call proceedApplication()
        classDefBy("Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;")
            .methods.first { it.name == "onCreate" }
            .toMutable()
            .addInstructions(
                0,
                """
                    # Save bundle to p1 for super call
                    move-object p1, p1
                    
                    # Call super.onCreate(savedInstanceState)
                    invoke-super {p0, p1}, Lcom/jio/jioplay/tv/base/BaseActivity;->onCreate(Landroid/os/Bundle;)V
                    
                    # Skip all checks - directly call proceedApplication() 
                    # This bypasses: isSupportedDevice, isValidBuild, isValidVersionName, 
                    # isRooted, BlueStacks folder, isEmulator, and leanback feature checks
                    invoke-virtual {p0}, Lcom/jio/media/tv/ui/permission_onboarding/PermissionActivity;->proceedApplication()V
                    
                    return-void
                """,
            )
    }
}
