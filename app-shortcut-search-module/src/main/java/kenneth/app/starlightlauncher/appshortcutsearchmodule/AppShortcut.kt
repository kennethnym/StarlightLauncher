package kenneth.app.starlightlauncher.appshortcutsearchmodule

import android.content.pm.LauncherActivityInfo
import android.content.pm.ShortcutInfo

data class AppShortcut(
    val info: ShortcutInfo,

    val app: LauncherActivityInfo?,
)
