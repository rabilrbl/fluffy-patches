package app.template.patches.jiotv.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import app.template.patches.shared.Constants.COMPATIBILITY_JIOTV_MOBILE

@Suppress("unused")
val forceInitPairipStringsPatch = bytecodePatch(
    name = "Force-initialize pairip string class",
    description = "Forces initialization of pkOPEgq (pairip string decryption class) before OkHttp loads. Without this, Header.clinit accesses null fields causing NoClassDefFoundError for Hpack.",
) {
    compatibleWith(COMPATIBILITY_JIOTV_MOBILE)

    execute {
        // In JioTVApplication.attachBaseContext(), after MultiDex.install(),
        // add: sget-object v0, Lcom/iab/omid/library/ril/HNCT/pkOPEgq;->hxbBMeNJrlJB:Ljava/lang/String;
        // This forces pkOPEgq.clinit to run before any OkHttp class loads.
        classDefBy("Lcom/jio/jioplay/tv/JioTVApplication;")
            .methods.first { it.name == "attachBaseContext" }
            .toMutable()
            .addInstructions(
                1, // After invoke-super and MultiDex.install
                "sget-object v0, Lcom/iab/omid/library/ril/HNCT/pkOPEgq;->hxbBMeNJrlJB:Ljava/lang/String;",
            )
    }
}