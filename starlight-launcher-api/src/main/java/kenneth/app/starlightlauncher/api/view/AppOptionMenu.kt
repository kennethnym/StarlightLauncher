package kenneth.app.starlightlauncher.api.view

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Build
import android.os.Process
import android.view.LayoutInflater
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import kenneth.app.starlightlauncher.api.IconPack
import kenneth.app.starlightlauncher.api.R
import kenneth.app.starlightlauncher.api.databinding.AppOptionMenuBinding

private const val MAX_APP_SHORTCUTS_SHOWN = 5

/**
 * A menu that is shown after long pressing an app icon to present to the user
 * a list of actions that can be performed on the app, such as uninstalling it or pinning it.
 * App shortcuts are also shown in the list.
 */
class AppOptionMenu(
    val context: Context,
    private val app: LauncherActivityInfo,
    private val iconPack: IconPack,
    private val isAppPinned: Boolean,
    private val menu: OptionMenu,
    private val callback: ActionCallback,
) {
    interface ActionCallback {
        fun onUninstallApp(app: LauncherActivityInfo)

        fun onPinApp(app: LauncherActivityInfo)

        fun onUnpinApp(app: LauncherActivityInfo)

        @RequiresApi(Build.VERSION_CODES.N_MR1)
        fun onOpenShortcut(shortcut: ShortcutInfo, sourceBound: Rect)
    }

    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    init {
        loadMenu()
    }

    private fun loadMenu() {
        AppOptionMenuBinding.inflate(LayoutInflater.from(context), menu, true).apply {
            val appIcon = iconPack.getIconOf(app)

            isAppPinned = this@AppOptionMenu.isAppPinned

            appOptionMenuAppLabel.text = app.label
            appOptionMenuAppIcon.contentDescription =
                context.getString(R.string.app_icon_content_description, app.label)
            Glide.with(context).load(appIcon).into(appOptionMenuAppIcon)

            pinAppItem.setOnClickListener {
                pinOrUnpinApp(shouldPin = isAppPinned != true)
                menu.hide()
            }

            uninstallItem.setOnClickListener {
                uninstallApp()
                menu.hide()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                loadShortcutList(this)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun loadShortcutList(binding: AppOptionMenuBinding) {
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
                ?.take(MAX_APP_SHORTCUTS_SHOWN)
                ?.run {
                    binding.hasAppShortcuts = isNotEmpty()

                    forEach { shortcutInfo ->
                        val shortcutIcon = launcherApps.getShortcutIconDrawable(
                            shortcutInfo,
                            context.resources.displayMetrics.densityDpi
                        )

                        val menuItem = menu.createItem(
                            shortcutIcon,
                            shortcutInfo.longLabel?.toString()
                                ?: shortcutInfo.shortLabel.toString(),
                            applyIconTint = false,
                        ) {
                            val sourceBounds = Rect().run {
                                it.iconView.getGlobalVisibleRect(this)
                                this
                            }
                            openShortcut(shortcutInfo, sourceBounds)
                        }

                        binding.appShortcutList.addView(menuItem)
                    }
                }
        } catch (ex: SecurityException) {
            // Starlight launcher has to be the default launcher app in order to query
            // app shortcuts. Otherwise, SecurityException is thrown.
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun openShortcut(shortcutInfo: ShortcutInfo, sourceBound: Rect) {
        callback.onOpenShortcut(shortcutInfo, sourceBound)
    }

    private fun uninstallApp() {
        callback.onUninstallApp(app)
    }

    /**
     * Pin or unpin the app.
     *
     * @param shouldPin whether the app should be pinned or not.
     */
    private fun pinOrUnpinApp(shouldPin: Boolean) {
        if (shouldPin) {
            callback.onPinApp(app)
        } else {
            callback.onUnpinApp(app)
        }
    }
}