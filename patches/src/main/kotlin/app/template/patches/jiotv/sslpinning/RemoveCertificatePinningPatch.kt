package app.rabil.patches.jiotv.sslpinning

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val removeCertificatePinningPatch = bytecodePatch(
    name = "Remove certificate pinning",
    description = "Removes SSL/TLS certificate pinning for tv.media.jio.com. " +
        "The app uses OkHttp3 CertificatePinner with SHA-256 pins controlled by Firebase Remote Config " +
        "(FirebaseConfig.isSslPining()). This patch disables the pinning toggle and also patches " +
        "the OkHttp3 CertificatePinner.check() method to prevent pin validation failures.",
) {
    compatibleWith("com.jio.jioplay.tv"("7.1.7"))

    execute {
        // --- FirebaseConfig.isSslPining() → always return false ---
        // In APIManager.getNormalHttpClient(), when this returns true, a CertificatePinner
        // is built with two SHA-256 pins for "tv.media.jio.com":
        //   sha256/8Rw90Ej3Ttt8RRkrg+WYDS9n7IS03bk5bjP/UXPtaY8=
        //   sha256/Ko8tivDrEjiY90yGasP6ZpBU4jwXvHqVvQI0GS3GNdA=
        // Returning false skips the certificatePinner() builder call entirely.
        findClass("Lcom/jio/jioplay/tv/data/firebase/FirebaseConfig;")!!
            .methods.first { it.name == "isSslPining" }
            .addInstructions(
                0,
                """
                    const/4 v0, 0x0
                    return v0
                """,
            )

        // --- OkHttp3 CertificatePinner.check() → return-void ---
        // Even if pins are somehow configured (e.g., via a different code path),
        // this prevents OkHttp from actually validating server certificates
        // against pinned hashes. Catches both check(String, List) and
        // check(String, Function0) overloads.
        findClass("Lokhttp3/CertificatePinner;")!!
            .methods.filter { it.name == "check" }
            .forEach { method ->
                method.addInstructions(0, "return-void")
            }

        // --- Legacy OkHttp CertificatePinner.check() → return-void ---
        // The app also bundles the older com.squareup.okhttp.CertificatePinner
        // (likely from an embedded SDK like Jio's media SDK). Patch for completeness.
        findClass("Lcom/squareup/okhttp/CertificatePinner;")?.let { classDef ->
            classDef.methods.filter { it.name == "check" }
                .forEach { method ->
                    method.addInstructions(0, "return-void")
                }
        }
    }
}
