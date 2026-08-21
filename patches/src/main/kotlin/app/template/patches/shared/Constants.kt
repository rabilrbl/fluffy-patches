package app.template.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_ALARMY = Compatibility(
        name = "Alarmy",
        packageName = "droom.sleepIfUCan",
        apkFileType = ApkFileType.APK,
        appIconColor = 0xFF6B4C,
        targets = listOf(
            AppTarget(
                version = "26.32.1",
            ),
        ),
    )

    val COMPATIBILITY_BLOCKER_X = Compatibility(
        name = "BlockerX",
        packageName = "io.funswitch.blocker",
        apkFileType = ApkFileType.APK,
        appIconColor = 0x5B4BDB,
        targets = listOf(
            AppTarget(
                version = "5.0.81",
            ),
        ),
    )
}
