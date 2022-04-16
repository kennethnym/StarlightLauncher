package kenneth.app.starlightlauncher.appsearchmodule.view

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Process
import android.view.LayoutInflater
import androidx.core.content.res.ResourcesCompat
import kenneth.app.starlightlauncher.api.view.OptionMenu
import kenneth.app.starlightlauncher.api.view.OptionMenuItem
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModulePreferences
import kenneth.app.starlightlauncher.appsearchmodule.R
import kenneth.app.starlightlauncher.appsearchmodule.databinding.AppOptionMenuBinding

internal class AppOptionMenu(
    private val context: Context,
    private val app: LauncherActivityInfo,
    private val menu: OptionMenu
) {
    //    private val binding = AppOptionMenuBinding.inflate(LayoutInflater.from(context), menu, true)
    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    private val prefs = AppSearchModulePreferences.getInstance(context)
    private var isAppPinned = prefs.isAppPinned(app)

    private val maxAppShortcutsShown =
        context.resources.getInteger(R.integer.max_app_shortcuts_shown)

    init {
        loadMenu()
    }

    private fun loadMenu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
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
                    ?.forEach {
                        val shortcutIcon = launcherApps.getShortcutIconDrawable(
                            it,
                            context.resources.displayMetrics.densityDpi
                        )

                        menu.addItem(
                            shortcutIcon,
                            it.longLabel?.toString() ?: it.shortLabel.toString(),
                            applyIconTint = false,
                        ) { }
                    }
            } catch (ex: SecurityException) {
                // Starlight launcher has to be the default launcher app in order to query
                // app shortcuts. Otherwise, SecurityException is thrown.
            }
        }

        menu.addItem(
            if (isAppPinned) null
            else ResourcesCompat.getDrawable(
                context.resources,
                R.drawable.ic_favorite,
                context.theme
            ),
            context.getString(
                if (isAppPinned) R.string.app_option_menu_item_label_unpin_app
                else R.string.app_option_menu_item_label_pin_app
            ),
            applyIconTint = true,
            ::pinOrUnpinApp
        )

        menu.addItem(
            ResourcesCompat.getDrawable(context.resources, R.drawable.ic_trash_alt, context.theme),
            context.getString(R.string.app_option_menu_item_label_uninstall),
        ) { uninstallApp() }
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

    private fun pinOrUnpinApp(item: OptionMenuItem) {
        if (isAppPinned) {
            isAppPinned = false
            prefs.removePinnedApp(app)
            item.apply {
                itemIcon = ResourcesCompat.getDrawable(
                    context.resources,
                    R.drawable.ic_favorite,
                    context.theme
                )
                itemLabel = context.getString(R.string.app_option_menu_item_label_pin_app)
            }
        } else {
            isAppPinned = true
            prefs.addPinnedApp(app)
            item.apply {
                itemIcon = null
                itemLabel = context.getString(R.string.app_option_menu_item_label_unpin_app)
            }
        }
        menu.hide()
    }
}