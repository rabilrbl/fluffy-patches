package app.template.patches.alarmy.pairip

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_ALARMY

private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

@Suppress("unused")
val bypassPairipPatch = resourcePatch(
    name = "Bypass pairip license verification",
    description = "Removes pairip components from the manifest and replaces the Application class with AlarmyApp to prevent license checks from running at startup.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        document("AndroidManifest.xml").use { doc ->
            val appElement = doc.getElementsByTagName("application").item(0) as org.w3c.dom.Element

            val currentName = appElement.getAttributeNS(ANDROID_NS, "name")
                .ifEmpty { appElement.getAttribute("android:name") }
                .ifEmpty { appElement.getAttribute("name") }

            if (currentName == "com.pairip.application.Application" || currentName.isEmpty()) {
                appElement.setAttributeNS(ANDROID_NS, "android:name", "droom.sleepIfUCan.AlarmyApp")
            }

            appElement.setAttributeNS(ANDROID_NS, "android:extractNativeLibs", "true")

            val providers = doc.getElementsByTagName("provider")
            for (i in providers.length - 1 downTo 0) {
                val provider = providers.item(i) as org.w3c.dom.Element
                val providerName = provider.getAttributeNS(ANDROID_NS, "name")
                    .ifEmpty { provider.getAttribute("android:name") }
                    .ifEmpty { provider.getAttribute("name") }
                if (providerName == "com.pairip.licensecheck.LicenseContentProvider") {
                    provider.parentNode.removeChild(provider)
                }
            }

            val activities = doc.getElementsByTagName("activity")
            for (i in activities.length - 1 downTo 0) {
                val activity = activities.item(i) as org.w3c.dom.Element
                val activityName = activity.getAttributeNS(ANDROID_NS, "name")
                    .ifEmpty { activity.getAttribute("android:name") }
                    .ifEmpty { activity.getAttribute("name") }
                if (activityName == "com.pairip.licensecheck.LicenseActivity") {
                    activity.parentNode.removeChild(activity)
                }
            }
        }
    }
}

@Suppress("unused")
val disablePairipContentProviderPatch = bytecodePatch(
    name = "Disable pairip content provider",
    description = "Patches LicenseContentProvider.onCreate() to return immediately without creating a LicenseClient, preventing the first license check entry point.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
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
    }
}

@Suppress("unused")
val disablePairipSignatureCheckPatch = bytecodePatch(
    name = "Disable pairip signature check",
    description = "Patches SignatureCheck.verifyIntegrity() to return immediately, preventing APK signature tampering detection.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        classDefBy("Lcom/pairip/SignatureCheck;")
            .methods.first { it.name == "verifyIntegrity" }
            .toMutable()
            .addInstructions(
                0,
                """
                    return-void
                """,
            )
    }
}

@Suppress("unused")
val disablePairipLicenseCheckPatch = bytecodePatch(
    name = "Disable pairip license check",
    description = "Patches LicenseClient.initializeLicenseCheck() to return immediately, preventing Google Play license verification.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "initializeLicenseCheck" }
            .toMutable()
            .addInstructions(
                0,
                """
                    return-void
                """,
            )
    }
}

@Suppress("unused")
val disablePairipPaywallPatch = bytecodePatch(
    name = "Disable pairip paywall",
    description = "Patches LicenseClient.startPaywallActivity() to return immediately, preventing the 'Get this app from Play' redirect.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "startPaywallActivity" }
            .toMutable()
            .addInstructions(
                0,
                """
                    return-void
                """,
            )
    }
}

@Suppress("unused")
val disablePairipErrorDialogPatch = bytecodePatch(
    name = "Disable pairip error dialog",
    description = "Patches LicenseClient.startErrorDialogActivity() and handleError() to return immediately, preventing error dialogs.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "startErrorDialogActivity" }
            .toMutable()
            .addInstructions(
                0,
                """
                    return-void
                """,
            )

        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "handleError" }
            .toMutable()
            .addInstructions(
                0,
                """
                    return-void
                """,
            )
    }
}

@Suppress("unused")
val disablePairipLicenseActivityPatch = bytecodePatch(
    name = "Disable pairip license activity",
    description = "Patches LicenseActivity.onStart() to finish immediately, closing the activity if it is somehow launched.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        classDefBy("Lcom/pairip/licensecheck/LicenseActivity;")
            .methods.first { it.name == "onStart" }
            .toMutable()
            .addInstructions(
                0,
                """
                    invoke-virtual {p0}, Landroid/app/Activity;->finish()V
                    return-void
                """,
            )
    }
}

@Suppress("unused")
val disablePairipResponseValidationPatch = bytecodePatch(
    name = "Disable pairip response validation",
    description = "Patches LicenseResponseHelper.validateResponse() to return immediately, causing any license check that gets through to be treated as valid.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        classDefBy("Lcom/pairip/licensecheck/LicenseResponseHelper;")
            .methods.first { it.name == "validateResponse" }
            .toMutable()
            .addInstructions(
                0,
                """
                    return-void
                """,
            )
    }
}
