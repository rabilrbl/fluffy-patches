package app.rabil.patches.jiotv.root

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val removeRootDetectionPatch = bytecodePatch(
    name = "Remove root detection",
    description = "Removes root detection checks. Bypasses Firebase CommonUtils.isRooted() which " +
        "checks Build.TAGS for 'test-keys', /system/app/Superuser.apk, and /system/xbin/su. " +
        "Also neutralizes SecurityUtils validation and the Xposed framework detection dialog.",
) {
    compatibleWith("com.jio.jioplay.tv"("7.1.7"))

    execute {
        // --- CommonUtils.isRooted() → always return false ---
        // Firebase Crashlytics root detection checks:
        //   1. isEmulator() || Build.TAGS contains "test-keys"
        //   2. /system/app/Superuser.apk exists
        //   3. !isEmulator() && /system/xbin/su exists
        // Used in PermissionActivity.onCreate() and PermissionActivity.D()
        // to block rooted devices with "Your device is not compatible with JioTV" toast.
        findClass("Lcom/google/firebase/crashlytics/internal/common/CommonUtils;")!!
            .methods.first { it.name == "isRooted" }
            .addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )

        // --- SecurityUtils.isValidBuild() → always return true ---
        // Currently returns true already, but patching for safety
        // in case a future update changes the behavior.
        findClass("Lcom/jio/jioplay/tv/utils/SecurityUtils;")!!
            .methods.first { it.name == "isValidBuild" }
            .addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )

        // --- SecurityUtils.isValidVersionName() → always return true ---
        // Validates version name format by parsing it as a Long.
        // Could fail on modified builds with non-numeric version names.
        findClass("Lcom/jio/jioplay/tv/utils/SecurityUtils;")!!
            .methods.first { it.name == "isValidVersionName" }
            .addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )

        // --- CommonUtils.showXposedFrameworkDetectionDialog() → return-void ---
        // Shows a blocking "Security Warning!!!" dialog with non-cancelable AlertDialog
        // when root/emulator/BlueStacks is detected. The dialog's OK button calls
        // finish() on PermissionActivity, killing the app. Neutering prevents this.
        findClass("Lcom/jio/jioplay/tv/utils/CommonUtils;")!!
            .methods.first { it.name == "showXposedFrameworkDetectionDialog" }
            .addInstructions(0, "return-void")
    }
}
