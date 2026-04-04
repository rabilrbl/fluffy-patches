package app.template.patches.jiotv.sslpinning

import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE
import app.morphe.patcher.patch.bytecodePatch

val removeCertificatePinningPatch = bytecodePatch(
    name = "Remove certificate pinning (DISABLED)",
    description = "DISABLED: Testing resource-only approach.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)
    execute {
        // No-op for testing
    }
}
