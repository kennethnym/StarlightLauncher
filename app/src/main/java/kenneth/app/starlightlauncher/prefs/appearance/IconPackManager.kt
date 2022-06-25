package kenneth.app.starlightlauncher.prefs.appearance

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val NOVA_LAUNCHER_INTENT_CATEGORY = "com.teslacoilsw.launcher.THEME"
private const val NOVA_LAUNCHER_INTENT_ACTION = "com.novalauncher.THEME"

internal typealias InstalledIconPacks = Map<String, InstalledIconPack>

@Singleton
internal class IconPackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager = context.packageManager

    /**
     * A list of intents that icon packs declare.
     * This is used to load installed icon packs that support
     * the following intents.
     */
    private val iconPackIntents = listOf(
        // icon pack that supports nova launcher
        Intent(Intent.ACTION_MAIN).apply {
            addCategory(NOVA_LAUNCHER_INTENT_CATEGORY)
        },
        Intent(NOVA_LAUNCHER_INTENT_ACTION),
    )

    fun queryInstalledIconPacks(): InstalledIconPacks = iconPackIntents
        .flatMap {
            packageManager.queryIntentActivities(it, PackageManager.GET_META_DATA)
        }
        .fold(mutableMapOf()) { iconPacks, resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            iconPacks.apply {
                put(packageName, InstalledIconPack(context, packageName))
            }
        }
}
