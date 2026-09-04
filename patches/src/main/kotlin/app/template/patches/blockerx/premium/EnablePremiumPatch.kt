package app.template.patches.blockerx.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_BLOCKER_X

@Suppress("unused")
val enablePremiumPatch = bytecodePatch(
    name = "Enable Premium",
    description = "Enables BlockerX premium gates and premium module access.",
) {
    compatibleWith(COMPATIBILITY_BLOCKER_X)

    execute {
        val preferencesClass = mutableClassDefBy("Lio/funswitch/blocker/utils/sharePrefUtils/BlockerXAppSharePref;")

        listOf(
            "getSUB_STATUS",
            "getSUB_STATUS_LITE",
            "getIS_ACTIVE_CODI_MODE_PREMIUM",
            "getIS_ACTIVE_DESKTOP_PREMIUM",
            "getIS_ACTIVE_ED_COURSE_PREMIUM",
            "getIS_ACTIVE_PREMIUM_PLUS",
            "getIS_ACTIVE_URGES_MODE_PREMIUM",
        ).forEach { methodName ->
            preferencesClass.methods
                .first { it.name == methodName && it.returnType == "Z" }
                .apply {
                    removeInstructions(0, instructions.count())
                    addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                }
        }
    }
}
