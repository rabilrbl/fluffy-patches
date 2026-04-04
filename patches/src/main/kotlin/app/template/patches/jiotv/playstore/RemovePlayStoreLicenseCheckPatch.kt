package app.template.patches.jiotv.playstore

import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

val disablePairipManifestPatch = resourcePatch(
    name = "Disable pairip license check (manifest)",
    description = "Removes pairip components and changes application class to bypass pairip initialization.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        document("AndroidManifest.xml").use { doc ->
            val appElement = doc.getElementsByTagName("application").item(0) as org.w3c.dom.Element
            
            // Change application class from pairip's wrapper to the real JioTVApplication
            val currentName = appElement.getAttribute("android:name")
            if (currentName == "com.pairip.application.Application") {
                appElement.setAttribute("android:name", "com.jio.jioplay.tv.JioTVApplication")
            }
            
            // Remove pairip components
            val removeNames = setOf(
                "com.pairip.licensecheck.LicenseContentProvider",
                "com.pairip.licensecheck.LicenseActivity",
                "com.google.android.play.core.common.PlayCoreDialogWrapperActivity",
            )
            for (tag in listOf("provider", "activity")) {
                val elements = doc.getElementsByTagName(tag)
                val toRemove = mutableListOf<org.w3c.dom.Element>()
                for (i in 0 until elements.length) {
                    val el = elements.item(i) as org.w3c.dom.Element
                    if (el.getAttribute("android:name") in removeNames) {
                        toRemove.add(el)
                    }
                }
                for (el in toRemove) {
                    el.parentNode.removeChild(el)
                }
            }
            
            // Remove pairip permission
            val usesPermissions = doc.getElementsByTagName("uses-permission")
            val permsToRemove = mutableListOf<org.w3c.dom.Element>()
            for (i in 0 until usesPermissions.length) {
                val el = usesPermissions.item(i) as org.w3c.dom.Element
                val name = el.getAttribute("android:name")
                if (name == "com.android.vending.CHECK_LICENSE") {
                    permsToRemove.add(el)
                }
            }
            for (el in permsToRemove) {
                el.parentNode.removeChild(el)
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
