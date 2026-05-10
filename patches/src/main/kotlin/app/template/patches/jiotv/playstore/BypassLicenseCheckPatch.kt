package app.template.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val bypassLicenseCheckPatch = bytecodePatch(
    name = "Bypass license check",
    description = "Bypasses pairip LicenseClientV3 license verification by preventing all license checks, paywall display, and error dialogs.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        // initializeLicenseCheck() → return-void
        // Prevents both CHECK_REQUIRED and OK paths from running
        classDefBy("Lcom/pairip/licensecheck3/LicenseClientV3;")
            .methods.first { it.name == "initializeLicenseCheck" }
            .toMutable()
            .addInstructions(0, "return-void")

        // processResponse() → set state to OK + return-void
        // Even if called, immediately sets license state to OK
        classDefBy("Lcom/pairip/licensecheck3/LicenseClientV3;")
            .methods.first { it.name == "processResponse" }
            .toMutable()
            .addInstructions(
                0,
                """
                    sget-object v0, Lcom/pairip/licensecheck3/LicenseClientV3${'$'}LicenseCheckState;->OK:Lcom/pairip/licensecheck3/LicenseClientV3${'$'}LicenseCheckState;
                    sput-object v0, Lcom/pairip/licensecheck3/LicenseClientV3;->licenseCheckState:Lcom/pairip/licensecheck3/LicenseClientV3${'$'}LicenseCheckState;
                    return-void
                """,
            )

        // showPaywall() → return-void
        classDefBy("Lcom/pairip/licensecheck3/LicenseClientV3;")
            .methods.first { it.name == "showPaywall" }
            .toMutable()
            .addInstructions(0, "return-void")

        // showErrorDialog() → return-void
        classDefBy("Lcom/pairip/licensecheck3/LicenseClientV3;")
            .methods.first { it.name == "showErrorDialog" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}