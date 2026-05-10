package app.template.patches.jiotv.misc

import app.morphe.patcher.patch.resourcePatch
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val removeSplitMetadataPatch = resourcePatch(
    name = "Remove split APK metadata",
    description = "Removes requiredSplitTypes and split-related meta-data from AndroidManifest.xml to allow standalone APK installation.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        document("AndroidManifest.xml").use { doc ->
            val manifest = doc.documentElement

            // Remove requiredSplitTypes and splitTypes attributes
            manifest.removeAttribute("android:requiredSplitTypes")
            manifest.removeAttribute("android:splitTypes")

            val application = doc.getElementsByTagName("application").item(0) as org.w3c.dom.Element

            // Remove split-related meta-data elements
            val metaDatas = application.getElementsByTagName("meta-data")
            val toRemove = mutableListOf<org.w3c.dom.Node>()
            for (i in 0 until metaDatas.length) {
                val meta = metaDatas.item(i) as org.w3c.dom.Element
                val name = meta.getAttribute("android:name")
                if (name == "com.android.vending.splits.required" ||
                    name == "com.android.vending.splits" ||
                    name == "com.android.vending.derived.apk.id"
                ) {
                    toRemove.add(meta)
                }
            }
            for (node in toRemove) {
                application.removeChild(node)
            }
        }
    }
}