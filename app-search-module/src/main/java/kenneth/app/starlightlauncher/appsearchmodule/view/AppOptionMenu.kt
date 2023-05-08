package kenneth.app.starlightlauncher.appsearchmodule.view

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Process
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.OptionMenu
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModulePreferences
import kenneth.app.starlightlauncher.appsearchmodule.R
import kenneth.app.starlightlauncher.appsearchmodule.databinding.AppOptionMenuBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * A menu that is shown after long pressing an app icon to present to the user
 * a list of actions that can be performed on the app, such as uninstalling it or pinning it.
 * App shortcuts are also shown in the list.
 */
internal class AppOptionMenu(
    private val context: Context,
    private val app: LauncherActivityInfo,
    private val launcher: StarlightLauncherApi,
    private val menu: OptionMenu
) {
    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val prefs = AppSearchModulePreferences.getInstance(launcher.dataStore)

    private val maxAppShortcutsShown =
        context.resources.getInteger(R.integer.max_app_shortcuts_shown)

    init {
        loadMenu()
    }

    private fun loadMenu() {
        AppOptionMenuBinding.inflate(LayoutInflater.from(context), menu, true).apply {
            val iconPack = runBlocking { launcher.iconPack.first() }
            val appIcon = iconPack.getIconOf(app)

            appOptionMenuAppLabel.text = app.label
            appOptionMenuAppIcon.contentDescription =
                context.getString(R.string.app_icon_content_description, app.label)
            Glide.with(context).load(appIcon).into(appOptionMenuAppIcon)

            pinAppItem.setOnClickListener {
                pinOrUnpinApp(shouldPin = isAppPinned != true)
            }

            uninstallItem.setOnClickListener {
                uninstallApp()
                menu.hide()
            }

            launcher.coroutineScope.launch {
                prefs.isAppPinned(app).collectLatest {
                    isAppPinned = it
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            loadShortcutList()
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun loadShortcutList() {
        try {
            launcherApps.getShortcuts(
                LauncherApps.ShortcutQuery().apply {
                    setActivity(app.componentName)
                    setQueryFlags(
                        LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                                LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST
                    )
                },
                Process.myUserHandle()
            )
                ?.sortedWith { shortcut1, shortcut2 ->
                    when {
                        shortcut1.isDeclaredInManifest && shortcut2.isDynamic -> -1
                        shortcut1.isDynamic && shortcut2.isDeclaredInManifest -> 1
                        else -> shortcut1.rank - shortcut2.rank
                    }
                }
                ?.take(maxAppShortcutsShown)
                ?.forEach { shortcutInfo ->
                    val shortcutIcon = launcherApps.getShortcutIconDrawable(
                        shortcutInfo,
                        context.resources.displayMetrics.densityDpi
                    )

                    menu.addItem(
                        shortcutIcon,
                        shortcutInfo.longLabel?.toString() ?: shortcutInfo.shortLabel.toString(),
                        applyIconTint = false,
                    ) {
                        val sourceBounds = Rect().run {
                            it.iconView.getGlobalVisibleRect(this)
                            this
                        }
                        openShortcut(shortcutInfo, sourceBounds)
                    }
                }
        } catch (ex: SecurityException) {
            // Starlight launcher has to be the default launcher app in order to query
            // app shortcuts. Otherwise, SecurityException is thrown.
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun openShortcut(shortcutInfo: ShortcutInfo, sourceBound: Rect) {
        launcherApps.startShortcut(shortcutInfo, sourceBound, null)
    }

    private fun uninstallApp() {
        context.startActivity(
            Intent(
                Intent.ACTION_DELETE,
                Uri.fromParts("package", app.applicationInfo.packageName, null)
            )
        )
    }

    /**
     * Pin or unpin the app.
     *
     * @param shouldPin whether the app should be pinned or not.
     */
    private fun pinOrUnpinApp(shouldPin: Boolean) {
        launcher.coroutineScope.launch {
            if (shouldPin) {
                prefs.addPinnedApp(app)
            } else {
                prefs.removePinnedApp(app)
            }
        }
    }
}