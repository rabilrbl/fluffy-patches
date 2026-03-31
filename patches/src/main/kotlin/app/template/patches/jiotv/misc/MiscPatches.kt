package app.rabil.patches.jiotv.misc

import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE
import org.w3c.dom.Element

@Suppress("unused")
val enableCleartextTrafficPatch = resourcePatch(
    name = "Enable cleartext traffic",
    description = "Sets usesCleartextTraffic to true in AndroidManifest and patches the network " +
        "security config to allow cleartext HTTP traffic and user-installed CA certificates. " +
        "This enables MITM proxy tools like mitmproxy, Charles, and HTTP Toolkit to intercept traffic.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

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
