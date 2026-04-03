package app.template.patches.jiotv.misc

import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

val miscPatches = resourcePatch(
    name = "Enable cleartext traffic",
    description = "Sets usesCleartextTraffic to true in AndroidManifest and patches the network security config to allow cleartext HTTP traffic and user-installed CA certificates. This enables MITM proxy tools like mitmproxy, Charles, and HTTP Toolkit to intercept traffic.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        document("AndroidManifest.xml").use { doc ->
            val appElement = doc.getElementsByTagName("application").item(0) as org.w3c.dom.Element
            appElement.setAttribute("android:usesCleartextTraffic", "true")
        }

        document("res/xml/network_security_config.xml").use { doc ->
            val root = doc.documentElement

            val existingChildren = mutableListOf<org.w3c.dom.Node>()
            var child = root.firstChild
            while (child != null) {
                existingChildren.add(child)
                child = child.nextSibling
            }
            for (node in existingChildren) {
                root.removeChild(node)
            }

            val baseConfig = doc.createElement("base-config")
            baseConfig.setAttribute("cleartextTrafficPermitted", "true")
            root.appendChild(baseConfig)

            val domainConfig = doc.createElement("domain-config")
            domainConfig.setAttribute("cleartextTrafficPermitted", "true")

            val trustAnchors = doc.createElement("trust-anchors")
            val certsSystem = doc.createElement("certificates")
            certsSystem.setAttribute("src", "system")
            trustAnchors.appendChild(certsSystem)
            val certsUser = doc.createElement("certificates")
            certsUser.setAttribute("src", "user")
            trustAnchors.appendChild(certsUser)

            domainConfig.appendChild(trustAnchors)
            root.appendChild(domainConfig)
        }
    }
}
