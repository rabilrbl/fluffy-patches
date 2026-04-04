package app.template.patches.jiotv.sslpinning

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val removeCertificatePinningPatch = bytecodePatch(
    name = "Remove certificate pinning",
    description = "Removes SSL/TLS certificate pinning for tv.media.jio.com.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        classDefBy("Lcom/jio/jioplay/tv/data/firebase/FirebaseConfig;")
            .methods.first { it.name == "isSslPining" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )

        classDefBy("Lokhttp3/CertificatePinner;")
            .methods.filter { it.name == "check" }
            .forEach { method ->
                method.toMutable().addInstructions(0, "return-void")
            }

        runCatching { classDefBy("Lcom/squareup/okhttp/CertificatePinner;") }.getOrNull()?.let { classDef ->
            classDef.methods.filter { it.name == "check" }
                .forEach { method ->
                    method.toMutable().addInstructions(0, "return-void")
                }
        }
    }
}
