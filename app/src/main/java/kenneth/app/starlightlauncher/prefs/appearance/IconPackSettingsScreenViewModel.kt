package kenneth.app.starlightlauncher.prefs.appearance

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.api.IconPack
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val NOVA_LAUNCHER_INTENT_CATEGORY = "com.teslacoilsw.launcher.THEME"
private const val NOVA_LAUNCHER_INTENT_ACTION = "com.novalauncher.THEME"

/**
 * A [ViewModel] for handling [IconPackSettingsScreen] data.
 */
@HiltViewModel
internal class IconPackSettingsScreenViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val appearancePreferenceManager: AppearancePreferenceManager,
) : ViewModel() {
    var selectedIconPack by mutableStateOf<IconPack?>(null)
        private set

    var installedIconPacks by mutableStateOf(listOf<InstalledIconPack>())
        private set

    private val packageManager = context.packageManager
    private val resources = context.resources

    init {
        querySelectedIconPack()
        queryIconPacks()
    }

    fun changeIconPack(iconPack: InstalledIconPack) {
        viewModelScope.launch {
            appearancePreferenceManager.changeIconPack(iconPack)
        }
    }

    private fun querySelectedIconPack() {
        viewModelScope.launch {
            appearancePreferenceManager.iconPack.collect {
                selectedIconPack = it
            }
        }
    }

    private fun queryIconPacks() {
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
            .distinctBy { it.activityInfo.packageName }
            .map { InstalledIconPack(it.activityInfo.packageName, packageManager, resources) }
            .also { installedIconPacks = it }
    }
}