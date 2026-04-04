package app.template.patches.jiotv.root

import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE
import app.morphe.patcher.patch.bytecodePatch

val removeRootDetectionPatch = bytecodePatch(
    name = "Remove root detection (DISABLED)",
    description = "DISABLED: Testing resource-only approach.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)
    execute {
        // No-op for testing
    }
}
