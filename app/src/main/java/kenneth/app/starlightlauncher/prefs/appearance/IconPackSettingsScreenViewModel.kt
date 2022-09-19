package kenneth.app.starlightlauncher.prefs.appearance

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kenneth.app.starlightlauncher.MainApplication
import kenneth.app.starlightlauncher.api.IconPack
import kenneth.app.starlightlauncher.dataStore
import kenneth.app.starlightlauncher.prefs.PREF_KEY_ICON_PACK
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private const val NOVA_LAUNCHER_INTENT_CATEGORY = "com.teslacoilsw.launcher.THEME"
private const val NOVA_LAUNCHER_INTENT_ACTION = "com.novalauncher.THEME"

/**
 * A [ViewModel] for handling [IconPackSettingsScreen] data.
 */
internal class IconPackSettingsScreenViewModel(application: Application) :
    AndroidViewModel(application) {
    var selectedIconPack by mutableStateOf<IconPack?>(null)
        private set

    var installedIconPacks by mutableStateOf(listOf<InstalledIconPack>())
        private set

    init {
        querySelectedIconPack()
        queryIconPacks()
    }

    private fun querySelectedIconPack() {
        val application = getApplication<MainApplication>()
        viewModelScope.launch {
            application.dataStore.data
                .map { it[PREF_KEY_ICON_PACK] }
                .collect { iconPackPackageName ->
                    selectedIconPack = if (iconPackPackageName == null) {
                        DefaultIconPack(application)
                    } else {
                        InstalledIconPack(application, iconPackPackageName)
                    }
                }
        }
    }

    private fun queryIconPacks() {
        val application = getApplication<MainApplication>()
        val packageManager = application.packageManager

        listOf(
            // icon pack that supports nova launcher
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(NOVA_LAUNCHER_INTENT_CATEGORY)
            },
            Intent(NOVA_LAUNCHER_INTENT_ACTION),
        )
            .flatMap {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    packageManager.queryIntentActivities(
                        it,
                        PackageManager.ResolveInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                    )
                else
                    packageManager.queryIntentActivities(it, PackageManager.GET_META_DATA)
            }
            .map { InstalledIconPack(application, it.activityInfo.packageName) }
            .also { installedIconPacks = it }
    }
}