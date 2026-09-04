package app.fluffy.patches.alarmy.premium

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.fluffy.patches.shared.Constants.COMPATIBILITY_ALARMY

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks premium features and disables ads by forcing both premium gates true.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        val premiumStateClass = mutableClassDefBy("Lbi/c;")

        premiumStateClass.methods.first { it.name == "r" && it.returnType == "Z" }
            .apply {
                removeInstructions(0, instructions.count())
                addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }

        premiumStateClass.methods.first { it.name == "s" && it.returnType == "Z" }
            .apply {
                removeInstructions(0, instructions.count())
                addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
    }
}
