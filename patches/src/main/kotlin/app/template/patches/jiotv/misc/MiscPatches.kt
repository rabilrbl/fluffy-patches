package app.rabil.patches.jiotv.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_EXAMPLE
import org.w3c.dom.Element

@Suppress("unused")
val enableCleartextTrafficPatch = resourcePatch(
    name = "Enable cleartext traffic",
    description = "Sets usesCleartextTraffic to true in AndroidManifest and patches the network " +
        "security config to allow cleartext HTTP traffic and user-installed CA certificates. " +
        "This enables MITM proxy tools like mitmproxy, Charles, and HTTP Toolkit to intercept traffic.",
) {
    compatibleWith(COMPATIBILITY_EXAMPLE)

    execute {
        // Patch AndroidManifest.xml: set usesCleartextTraffic=true
        // The original manifest has android:usesCleartextTraffic="false"
        document("AndroidManifest.xml").use { document ->
            val application = document.getElementsByTagName("application").item(0) as Element
            application.setAttribute("android:usesCleartextTraffic", "true")
        }

        // Replace network_security_config.xml to trust user CA certs and allow cleartext.
        // The original config likely only trusts system CAs. Adding "user" source
        // allows user-installed certificates (e.g., mitmproxy CA) to be trusted.
        document("res/xml/network_security_config.xml").use { document ->
            val root = document.documentElement

            // Clear existing content
            while (root.hasChildNodes()) {
                root.removeChild(root.firstChild)
            }

            // Add base-config that trusts user certificates
            val baseConfig = document.createElement("base-config").apply {
                setAttribute("cleartextTrafficPermitted", "true")
            }
            val trustAnchors = document.createElement("trust-anchors")
            val systemCerts = document.createElement("certificates").apply {
                setAttribute("src", "system")
            }
            val userCerts = document.createElement("certificates").apply {
                setAttribute("src", "user")
            }
            trustAnchors.appendChild(systemCerts)
            trustAnchors.appendChild(userCerts)
            baseConfig.appendChild(trustAnchors)
            root.appendChild(baseConfig)
        }
    }
}

@Suppress("unused")
val enableDebuggingPatch = bytecodePatch(
    name = "Enable debugging",
    description = "Sets SecurityUtils.isDebug to true, which bypasses all security checks " +
        "in PermissionActivity.D() (root, emulator, build validation). The very first check " +
        "in that method is: if (!SecurityUtils.isDebug && (!isSupportedDevice || ...)) " +
        "so setting isDebug=true skips the entire security gate.",
) {
    compatibleWith(COMPATIBILITY_EXAMPLE)

    // Include root and emulator patches for defense-in-depth (they also
    // patch the individual checks that D() calls, as a redundant layer).
    dependsOn(
        app.rabil.patches.jiotv.root.removeRootDetectionPatch,
        app.rabil.patches.jiotv.emulator.removeEmulatorDetectionPatch,
    )

    execute {
        // --- Set SecurityUtils.isDebug = true ---
        // SecurityUtils has a static boolean field: public static boolean isDebug = false;
        // In PermissionActivity.D(), the first condition is:
        //   if (!SecurityUtils.isDebug && (!isSupportedDevice || !isValidBuild() ||
        //       !isValidVersionName() || CommonUtils.isRooted()))
        // When isDebug is true, this entire block is skipped, meaning the app proceeds
        // directly to permission handling regardless of device state.
        //
        // We patch the class initializer (<clinit>) to set isDebug = true at class load time.
        val securityUtilsClass = classDefBy("Lcom/jio/jioplay/tv/utils/SecurityUtils;")

        // If there's a static initializer, prepend our instruction
        val clinit = securityUtilsClass.methods.firstOrNull { it.name == "<clinit>" }
        if (clinit != null) {
            clinit.toMutable().addInstructions(
                0,
                """
                    const/4 v0, 0x1
                    sput-boolean v0, Lcom/jio/jioplay/tv/utils/SecurityUtils;->isDebug:Z
                """,
            )
        }
    }
}
