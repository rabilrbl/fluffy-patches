package app.rabil.patches.jiotv.root

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val removeRootDetectionPatch = bytecodePatch(
    name = "Remove root detection",
    description = "Removes root detection checks. Bypasses Firebase CommonUtils.isRooted() which " +
        "checks Build.TAGS for 'test-keys', /system/app/Superuser.apk, and /system/xbin/su. " +
        "Also neutralizes SecurityUtils validation and the Xposed framework detection dialog.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        // --- CommonUtils.isRooted() → always return false ---
        // Firebase Crashlytics root detection checks:
        //   1. isEmulator() || Build.TAGS contains "test-keys"
        //   2. /system/app/Superuser.apk exists
        //   3. !isEmulator() && /system/xbin/su exists
        // Used in PermissionActivity.onCreate() and PermissionActivity.D()
        // to block rooted devices with "Your device is not compatible with JioTV" toast.
        classDefBy("Lcom/google/firebase/crashlytics/internal/common/CommonUtils;")
            .methods.first { it.name == "isRooted" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )

        // --- SecurityUtils.isValidVersionName() → always return true ---
        // Validates version name format by parsing it as a Long.
        // Could fail on modified builds with non-numeric version names.
        classDefBy("Lcom/jio/jioplay/tv/utils/SecurityUtils;")
            .methods.first { it.name == "isValidVersionName" }
            .toMutable()
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
        classDefBy("Lcom/jio/jioplay/tv/utils/CommonUtils;")
            .methods.first { it.name == "showXposedFrameworkDetectionDialog" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}
