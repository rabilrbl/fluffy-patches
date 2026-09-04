package app.fluffy.patches.blockerx.instantapproval

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.fluffy.patches.shared.Constants.COMPATIBILITY_BLOCKER_X

@Suppress("unused")
val bypassInstantApprovalPatch = bytecodePatch(
    name = "Bypass Instant Approval",
    description = "Bypasses the local coin redemption step for Instant Approval actions.",
) {
    compatibleWith(COMPATIBILITY_BLOCKER_X)

    execute {
        mutableClassDefBy(
            "Lio/funswitch/blocker/features/switchPage/switchPages/main/dialogs/paidBuddyRequest/SwitchPageInstantAPApprovalDialog;",
        ).methods
            .first { it.name == "redeemCoinsForInstantApproval$1" && it.returnType == "V" }
            .apply {
                removeInstructions(0, instructions.count())
                addInstructions(
                    0,
                    """
                        sget-object v0, Lo/removeOnItemTouchListener;->r8lambda4IRRzyoWeWaykEOcgWGjbNoGAkw:Lo/removeOnItemTouchListener;
                        iget-object v1, p0, Lio/funswitch/blocker/features/switchPage/switchPages/main/dialogs/paidBuddyRequest/SwitchPageInstantAPApprovalDialog;->onClick:Lkotlin/jvm/functions/Function1;
                        if-eqz v1, :cond_0
                        invoke-interface {v1, v0}, Lkotlin/jvm/functions/Function1;->invoke(Ljava/lang/Object;)Ljava/lang/Object;
                        :cond_0
                        invoke-virtual {p0}, Lio/funswitch/blocker/features/switchPage/switchPages/main/dialogs/paidBuddyRequest/SwitchPageInstantAPApprovalDialog;->onDismissClick()V
                        return-void
                    """,
                )
            }
    }
}
