package kenneth.app.starlightlauncher.appsearchmodule.view

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.view.LayoutInflater
import kenneth.app.starlightlauncher.api.view.OptionMenu
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModulePreferences
import kenneth.app.starlightlauncher.appsearchmodule.databinding.AppOptionMenuBinding

internal class AppOptionMenu(
    private val context: Context,
    private val app: ActivityInfo,
    private val menu: OptionMenu
) {
    private val binding = AppOptionMenuBinding.inflate(LayoutInflater.from(context), menu, true)
    private val prefs = AppSearchModulePreferences.getInstance(context)

    init {
        with(binding) {
            isAppPinned = prefs.isAppPinned(app)
            uninstallItem.setOnClickListener { uninstallApp() }
            pinAppItem.setOnClickListener { pinOrUnpinApp() }
        }
    }

    private fun uninstallApp() {
        context.startActivity(
            Intent(
                Intent.ACTION_DELETE,
                Uri.fromParts("package", app.packageName, null)
            )
        )
        menu.hide()
    }

    private fun pinOrUnpinApp() {
        if (binding.isAppPinned == true) {
            prefs.removePinnedApp(app)
            binding.isAppPinned = false
        } else {
            prefs.addPinnedApp(app)
            binding.isAppPinned = true
        }
        menu.hide()
    }
}