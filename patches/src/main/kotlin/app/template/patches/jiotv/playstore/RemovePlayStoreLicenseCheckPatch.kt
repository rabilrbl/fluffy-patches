package app.rabil.patches.jiotv.playstore

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_EXAMPLE

@Suppress("unused")
val removePlayStoreLicenseCheckPatch = bytecodePatch(
    name = "Remove Play Store license check",
    description = "Removes the Play Store installation and license verification check (pairip). " +
        "Bypasses LicenseClient, SignatureCheck, and StartupLauncher VM execution.",
) {
    compatibleWith(COMPATIBILITY_EXAMPLE)

    execute {
        // --- Bypass LicenseClient.initializeLicenseCheck() ---
        // Main entry point for pairip's Play Store license verification.
        // Connects to com.android.vending licensing service and validates the purchase.
        // Making it return-void immediately prevents all license checks.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "initializeLicenseCheck" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Bypass connectToLicensingService() ---
        // Prevents binding to com.android.vending licensing service.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "connectToLicensingService" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Bypass performLocalInstallerCheck() → always return true ---
        // Checks getInstallSourceInfo() for "com.android.vending" as the installer.
        // For sideloaded APKs this fails. We make it always return true.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "performLocalInstallerCheck" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )

        // --- Neuter LicenseActivity.onStart() ---
        // Shows error dialogs and paywall intents when license check fails.
        // Preventing it from executing ensures no blocking UI is shown.
        classDefBy("Lcom/pairip/licensecheck/LicenseActivity;")
            .methods.first { it.name == "onStart" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Block popup launchers in LicenseClient ---
        // These two methods are responsible for opening blocking UI.
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "startErrorDialogActivity" }
            .toMutable()
            .addInstructions(0, "return-void")

        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "startPaywallActivity" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Disable content provider bootstrap ---
        // LicenseContentProvider.onCreate() eagerly triggers license init on app startup.
        classDefBy("Lcom/pairip/licensecheck/LicenseContentProvider;")
            .methods.first { it.name == "onCreate" }
            .toMutable()
            .addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    return v0
                """,
            )

        // --- Bypass SignatureCheck.verifyIntegrity() ---
        // Verifies APK signature hash against hardcoded SHA-256 values:
        //   expectedSignature = "VkwE0TgslZMpxvR+ldSXr9FRIQ5NlCaBT+tvpXr3rTA="
        //   ALLOWLISTED_SIG   = "Vn3kj4pUblROi2S+QfRRL9nhsaO2uoHQg6+dpEtxdTE="
        // Patched/re-signed APKs have different signatures → SignatureTamperedException.
        classDefBy("Lcom/pairip/SignatureCheck;")
            .methods.first { it.name == "verifyIntegrity" }
            .toMutable()
            .addInstructions(0, "return-void")

        // --- Bypass StartupLauncher.launch() ---
        // Loads encrypted VM bytecode (file "mVBwD2didVTgj5k7") from assets/ and
        // executes it via native libpairipcore.so through VMRunner.invoke().
        // The VM bytecode performs additional integrity checks at runtime.
        classDefBy("Lcom/pairip/StartupLauncher;")
            .methods.first { it.name == "launch" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}
