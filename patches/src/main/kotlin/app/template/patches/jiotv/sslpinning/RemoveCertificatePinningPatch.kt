package app.template.patches.jiotv.sslpinning

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val removeCertificatePinningPatch = bytecodePatch(
    name = "Remove certificate pinning",
    description = "Removes SSL/TLS certificate pinning for tv.media.jio.com and media.jio.com.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        // Bypass FirebaseConfig.isSslPining() — always return false
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

        // Bypass OkHttp CertificatePinner.check() — always return immediately
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

        // Bypass JioPlayer (k.c) manual cert pin verification for media.jio.com
        // The URL.contains("media.jio.com") check gates a manual X509TrustManagerExtensions
        // checkServerTrusted call with hardcoded SHA-256 pins. We skip the entire block.
        runCatching {
            classDefBy("Lcom/jio/jioplayer/k/c;")
                .methods.first { it.name == "a" && it.parameterTypes.size >= 7 }
                .toMutable()
                .addInstructions(0, "return-void")
        }
    }
}
