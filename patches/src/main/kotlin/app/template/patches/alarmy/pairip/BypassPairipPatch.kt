package app.template.patches.alarmy.pairip

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_ALARMY

@Suppress("unused")
val bypassPairipPatch = resourcePatch(
    name = "Bypass pairip license verification",
    description = "Replaces pairip Application with the real AlarmyApp, removes the LicenseContentProvider, and sets extractNativeLibs=true to prevent signature and license checks from running at startup.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        document("AndroidManifest.xml").use { doc ->
            val appElement = doc.getElementsByTagName("application").item(0) as org.w3c.dom.Element
            appElement.setAttribute("android:extractNativeLibs", "true")
            val currentName = appElement.getAttribute("android:name")
            if (currentName == "com.pairip.application.Application") {
                appElement.setAttribute("android:name", "droom.sleepIfUCan.AlarmyApp")
            }

            val providers = doc.getElementsByTagName("provider")
            for (i in providers.length - 1 downTo 0) {
                val provider = providers.item(i) as org.w3c.dom.Element
                val providerName = provider.getAttribute("android:name")
                if (providerName == "com.pairip.licensecheck.LicenseContentProvider") {
                    provider.parentNode.removeChild(provider)
                }
            }
        }
    }
}

@Suppress("unused")
val disableSignatureCheckPatch = bytecodePatch(
    name = "Disable pairip signature check",
    description = "Patches SignatureCheck.verifyIntegrity() to return immediately, preventing APK signature tampering detection.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        classDefBy("Lcom/pairip/SignatureCheck;")
            .methods.first { it.name == "verifyIntegrity" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}

@Suppress("unused")
val disableLicenseCheckPatch = bytecodePatch(
    name = "Disable pairip license check",
    description = "Patches LicenseClient.initializeLicenseCheck() to return immediately, preventing Google Play license verification.",
) {
    compatibleWith(COMPATIBILITY_ALARMY)

    execute {
        classDefBy("Lcom/pairip/licensecheck/LicenseClient;")
            .methods.first { it.name == "initializeLicenseCheck" }
            .toMutable()
            .addInstructions(0, "return-void")
    }
}
