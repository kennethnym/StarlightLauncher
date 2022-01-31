package kenneth.app.starlightlauncher.appsearchmodule

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.starlightlauncher.api.SpotlightLauncherApi
import kenneth.app.starlightlauncher.api.view.OptionMenu
import kenneth.app.starlightlauncher.appsearchmodule.databinding.AppGridItemBinding
import kenneth.app.starlightlauncher.appsearchmodule.databinding.AppOptionMenuBinding


internal class AppGridAdapter(
    context: Context,
    internal val apps: MutableList<ResolveInfo>,
    private val launcher: SpotlightLauncherApi
) : RecyclerView.Adapter<AppGridItem>() {
    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val appSearchModulePreferences = AppSearchModulePreferences.getInstance(context)

    private lateinit var selectedApp: ResolveInfo

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppGridItem {
        val binding = AppGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppGridItem(binding)
    }

    override fun onBindViewHolder(holder: AppGridItem, position: Int) {
        val app = apps[position]
        val appName = app.loadLabel(launcher.context.packageManager)

        with(holder.binding) {
            appIcon.apply {
                contentDescription =
                    context.getString(R.string.app_icon_content_description, appName)
                setImageBitmap(launcher.getIconPack().getIconOf(app))
            }

            if (appSearchModulePreferences.shouldShowAppLabels) {
                appLabel.apply {
                    isVisible = true
                    text = appName
                }
            } else {
                appLabel.isVisible = false
            }

            with(root) {
                setOnLongClickListener {
                    selectedApp = app
                    showAppOptionMenu()
                }

                setOnClickListener {
                    selectedApp = app
                    openSelectedApp()
                }
            }
        }

        sharedPreferences.registerOnSharedPreferenceChangeListener(holder)
    }

    override fun getItemCount(): Int = apps.size

    private fun showAppOptionMenu(): Boolean {
        launcher.showOptionMenu(::createAppOptionMenu)
        return true
    }

    private fun openSelectedApp() {
        launcher.context.startActivity(
            launcher.context.packageManager.getLaunchIntentForPackage(selectedApp.activityInfo.packageName)
        )
    }

    private fun createAppOptionMenu(menu: OptionMenu) {
        AppOptionMenuBinding.inflate(LayoutInflater.from(launcher.context), menu).also {
            it.uninstallItem.setOnClickListener { uninstallApp() }
        }
    }

    private fun uninstallApp() {
        launcher.context.startActivity(
            Intent(
                Intent.ACTION_DELETE,
                Uri.fromParts("package", selectedApp.activityInfo.packageName, null)
            )
        )
    }
}

internal class AppGridItem(internal val binding: AppGridItemBinding) :
    RecyclerView.ViewHolder(binding.root),
    SharedPreferences.OnSharedPreferenceChangeListener {
    private val prefs = AppSearchModulePreferences.getInstance(itemView.context)

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            prefs.keys.showAppLabels -> {
                binding.appLabel.isVisible = prefs.shouldShowAppLabels
            }
        }
    }
}