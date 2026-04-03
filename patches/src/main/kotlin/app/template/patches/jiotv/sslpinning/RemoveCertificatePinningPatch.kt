package app.template.patches.jiotv.sslpinning

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

val removeCertificatePinningPatch = bytecodePatch(
    name = "Remove certificate pinning",
    description = "Removes SSL/TLS certificate pinning for tv.media.jio.com. The app uses OkHttp3 CertificatePinner with SHA-256 pins controlled by Firebase Remote Config. This patch disables the pinning toggle and also patches the OkHttp3 CertificatePinner.check() method.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        mutableClassDefBy("Lcom/jio/jioplay/tv/p037tv/data/firebase/FirebaseConfig;")
            .directMethods
            .first { it.name == "isSslPining" }
            .addInstructions(0, "const/4 v0, 0x0\nreturn v0")

        mutableClassDefBy("Lokhttp3/CertificatePinner;")
            .directMethods
            .first { it.name == "check\$okhttp" }
            .addInstructions(0, "return-void")

        runCatching {
            mutableClassDefBy("Lcom/squareup/okhttp/CertificatePinner;")
                .directMethods
                .first { it.name == "check" }
                .addInstructions(0, "return-void")
        }
    }
}
