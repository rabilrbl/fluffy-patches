package app.template.patches.jiotv.root

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

val removeRootDetectionPatch = bytecodePatch(
    name = "Remove root detection",
    description = "Removes root detection checks. Bypasses Firebase CommonUtils.isRooted() which checks Build.TAGS for 'test-keys', /system/app/Superuser.apk, and /system/xbin/su. Also neutralizes SecurityUtils validation and the Xposed framework detection dialog.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        mutableClassDefBy("Lcom/google/firebase/crashlytics/internal/common/CommonUtils;")
            .directMethods
            .first { it.name == "isRooted" }
            .addInstructions(0, "const/4 v0, 0x0\nreturn v0")

        mutableClassDefBy("Lcom/jio/jioplay/tv/p037tv/utils/SecurityUtils;")
            .directMethods
            .first { it.name == "isValidVersionName" }
            .addInstructions(0, "const/4 v0, 0x1\nreturn v0")

        mutableClassDefBy("Lcom/jio/jioplay/tv/p037tv/utils/CommonUtils;")
            .directMethods
            .first { it.name == "showXposedFrameworkDetectionDialog" }
            .addInstructions(0, "return-void")
    }
}
