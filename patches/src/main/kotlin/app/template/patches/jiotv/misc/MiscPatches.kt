package app.template.patches.jiotv.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val miscPatches = resourcePatch(
    name = "Enable cleartext traffic",
    description = "Sets usesCleartextTraffic to true in AndroidManifest and patches the network security config to allow cleartext HTTP traffic and user-installed CA certificates.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        document("AndroidManifest.xml").use { doc ->
            val appElement = doc.getElementsByTagName("application").item(0) as org.w3c.dom.Element
            appElement.setAttribute("android:usesCleartextTraffic", "true")
            val currentName = appElement.getAttribute("android:name")
            if (currentName == "com.pairip.application.Application") {
                appElement.setAttribute("android:name", "com.jio.jioplay.tv.JioTVApplication")
            }
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
            val baseTrustAnchors = doc.createElement("trust-anchors")
            val baseCertsSystem = doc.createElement("certificates")
            baseCertsSystem.setAttribute("src", "system")
            baseTrustAnchors.appendChild(baseCertsSystem)
            val baseCertsUser = doc.createElement("certificates")
            baseCertsUser.setAttribute("src", "user")
            baseTrustAnchors.appendChild(baseCertsUser)
            baseConfig.appendChild(baseTrustAnchors)
            root.appendChild(baseConfig)

            val domainConfig = doc.createElement("domain-config")
            domainConfig.setAttribute("cleartextTrafficPermitted", "true")
            val domain = doc.createElement("domain")
            domain.setAttribute("includeSubdomains", "true")
            domain.appendChild(doc.createTextNode("tv.media.jio.com"))
            domainConfig.appendChild(domain)
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

@Suppress("unused")
val disableFirebaseInitPatch = resourcePatch(
    name = "Disable FirebaseInitProvider",
    description = "Removes FirebaseInitProvider from AndroidManifest.xml to prevent crash when VM config data is missing.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        document("AndroidManifest.xml").use { doc ->
            val application = doc.getElementsByTagName("application").item(0) as org.w3c.dom.Element
            val providers = application.getElementsByTagName("provider")
            val toRemove = mutableListOf<org.w3c.dom.Node>()
            for (i in 0 until providers.length) {
                val provider = providers.item(i) as org.w3c.dom.Element
                val name = provider.getAttribute("android:name")
                if (name.contains("FirebaseInitProvider")) {
                    toRemove.add(provider)
                }
            }
            for (node in toRemove) {
                application.removeChild(node)
            }
        }
    }
}
