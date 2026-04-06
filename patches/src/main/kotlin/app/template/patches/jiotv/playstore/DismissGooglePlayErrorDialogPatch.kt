package app.template.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val removePairipNativePatch = resourcePatch(
    name = "Remove pairip native library",
    description = "Removes libpairipcore.so from all architectures to prevent native signature verification that crashes on patched APKs.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        delete("lib/arm64-v8a/libpairipcore.so")
        delete("lib/armeabi-v7a/libpairipcore.so")
        delete("lib/x86/libpairipcore.so")
        delete("lib/x86_64/libpairipcore.so")
    }
}

@Suppress("unused")
val bypassPairipPatch = bytecodePatch(
    name = "Bypass pairip license check",
    description = "Neutralizes pairip license verification by preventing VMRunner execution and suppressing license check errors.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
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

        classDefBy("Lcom/pairip/StartupLauncher;")
            .methods.first { it.name == "launch" }
            .toMutable()
            .addInstructions(0, "return-void")

        classDefBy("Lcom/pairip/licensecheck3/LicenseClientV3;")
            .methods.first { it.name == "handleError" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}