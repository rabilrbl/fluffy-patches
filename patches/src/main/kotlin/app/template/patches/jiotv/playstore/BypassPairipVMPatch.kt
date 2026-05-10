package app.template.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val bypassPairipVMPatch = bytecodePatch(
    name = "Bypass pairip VM",
    description = "Bypasses pairip integrity verification VM by preventing StartupLauncher from invoking VMRunner, which avoids loading libpairipcore.so and executing signature checks.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        // Prevent StartupLauncher.launch() from calling VMRunner.invoke().
        // This avoids VMRunner.<clinit> which loads libpairipcore.so,
        // and prevents the pairip VM from running signature checks.
        classDefBy("Lcom/pairip/StartupLauncher;")
            .methods.first { it.name == "launch" }
            .toMutable()
            .addInstructions(0, "return-void")

        // Backup: make VMRunner.invoke() return null immediately.
        // If anything else calls invoke(), it won't trigger the native lib load.
        runCatching {
            classDefBy("Lcom/pairip/VMRunner;")
                .methods.first { it.name == "invoke" }
                .toMutable()
                .addInstructions(
                    0,
                    """
                        const/4 v0, 0x0
                        return-object v0
                    """,
                )
        }

        // Prevent SignatureCheck from throwing on modified APKs
        classDefBy("Lcom/pairip/SignatureCheck;")
            .methods.first { it.name == "verifyIntegrity" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}