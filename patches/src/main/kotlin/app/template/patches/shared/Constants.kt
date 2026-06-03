package app.template.patches.shared

import app.morphe.patcher.patch.ApkFileType
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility

object Constants {
    val COMPATIBILITY_ALARMY = Compatibility(
        name = "Alarmy",
        packageName = "droom.sleepIfUCan",
        apkFileType = ApkFileType.XAPK,
        appIconColor = 0xFF6B4C,
        targets = listOf(
            AppTarget(
                version = "26.23.0",
            ),
        ),
    )
}
