package kenneth.app.starlightlauncher.api

import android.content.ComponentName
import android.content.pm.LauncherActivityInfo

interface AppManager {
    fun launcherActivityInfoOf(componentName: ComponentName): LauncherActivityInfo
}