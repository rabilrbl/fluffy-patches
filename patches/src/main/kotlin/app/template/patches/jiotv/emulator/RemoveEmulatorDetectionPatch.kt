package app.template.patches.jiotv.emulator

import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE
import app.morphe.patcher.patch.bytecodePatch

val removeEmulatorDetectionPatch = bytecodePatch(
    name = "Remove emulator detection (DISABLED)",
    description = "DISABLED: Testing resource-only approach.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)
    execute {
        // No-op for testing
    }
}
