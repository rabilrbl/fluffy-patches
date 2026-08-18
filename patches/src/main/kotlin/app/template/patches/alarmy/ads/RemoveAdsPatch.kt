package app.template.patches.alarmy.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_ALARMY

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove Ads",
    description = "Removes all ads by forcing ad-free status to true.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        classDefBy("Lbi/PremiumState;")
            .methods.first { it.name == "s" && it.returnType == "Z" }
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
