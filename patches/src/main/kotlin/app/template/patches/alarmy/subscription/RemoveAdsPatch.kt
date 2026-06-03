package app.template.patches.alarmy.subscription

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_ALARMY

@Suppress("unused")
val removeAdsPatch = bytecodePatch(
    name = "Remove ads",
    description = "Bypasses ad-removal premium checks by forcing PremiumState.isRemoveAdPremium() to always return true.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        classDefBy("Lpi/b;")
            .methods.first { it.name == "s" }
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
