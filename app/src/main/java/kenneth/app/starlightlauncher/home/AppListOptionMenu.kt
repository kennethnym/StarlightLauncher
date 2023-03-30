package kenneth.app.starlightlauncher.home

import android.content.Context
import android.content.Intent
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.OptionMenu
import kenneth.app.starlightlauncher.api.view.OptionMenuItem
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModuleSettingsProvider
import kenneth.app.starlightlauncher.appsearchmodule.R
import kenneth.app.starlightlauncher.dataStore
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kotlinx.coroutines.launch

/**
 * A menu that is shown after long pressing an app icon to present to the user
 * a list of actions that can be performed on the app, such as uninstalling it or pinning it.
 * App shortcuts are also shown in the list.
 */
internal class AppListOptionMenu(
    context: Context,
    private val app: LauncherActivityInfo,
    private val launcher: StarlightLauncherApi,
    extensionManager: ExtensionManager,
    private val menu: OptionMenu
) {
    private val context = context.applicationContext
    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val prefs =
        (extensionManager.lookupExtension("kenneth.app.starlightlauncher.appsearchmodule")!!.settingsProvider as AppSearchModuleSettingsProvider).preferences(
            context.dataStore
        )

    private val maxAppShortcutsShown =
        context.resources.getInteger(R.integer.max_app_shortcuts_shown)

    private var pinAppMenuItem: OptionMenuItem? = null

    init {
        loadMenu()
    }

    private fun loadMenu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            loadShortcutList()
        }

        launcher.coroutineScope.launch {
            updatePinAppMenuItem()
        }

        menu.addItem(
            ResourcesCompat.getDrawable(context.resources, R.drawable.ic_trash_alt, context.theme),
            context.getString(R.string.app_option_menu_item_label_uninstall),
        ) { uninstallApp() }
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

    /**
     * Subscribes to whether the app is pinned or not,
     * and automatically update the pin app menu item accordingly.
     */
    private suspend fun updatePinAppMenuItem() {
        prefs.isAppPinned(app).collect { isPinned ->
            pinAppMenuItem?.let {
                if (isPinned) {
                    it.apply {
                        itemIcon = null
                        itemLabel =
                            context.getString(R.string.app_option_menu_item_label_unpin_app)
                    }
                } else {
                    it.apply {
                        itemIcon = ResourcesCompat.getDrawable(
                            context.resources,
                            R.drawable.ic_favorite,
                            context.theme
                        )
                        itemLabel =
                            context.getString(R.string.app_option_menu_item_label_pin_app)
                    }
                }
            } ?: menu.addItem(
                if (isPinned) null
                else ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.ic_favorite,
                    context.theme
                ),
                context.getString(
                    if (isPinned) R.string.app_option_menu_item_label_unpin_app
                    else R.string.app_option_menu_item_label_pin_app
                ),
                applyIconTint = true,
            ) {
                pinOrUnpinApp(shouldPin = !isPinned)
                menu.hide()
            }.also { pinAppMenuItem = it }
        }
    }

    private fun uninstallApp() {
        context.startActivity(
            Intent(
                Intent.ACTION_DELETE,
                Uri.fromParts("package", app.applicationInfo.packageName, null)
            )
        )
        menu.hide()
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