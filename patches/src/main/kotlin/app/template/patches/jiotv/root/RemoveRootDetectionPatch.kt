package app.template.patches.jiotv.root

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val removeRootDetectionPatch = bytecodePatch(
    name = "Remove root detection",
    description = "Removes root detection checks. Bypasses Firebase CommonUtils.isRooted() and Xposed framework detection.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
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

        classDefBy("Lcom/jio/jioplay/tv/utils/SecurityUtils;")
            .methods.first { it.name == "isValidBuild" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )

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

        classDefBy("Lcom/jio/jioplay/tv/utils/CommonUtils;")
            .methods.first { it.name == "showXposedFrameworkDetectionDialog" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}
