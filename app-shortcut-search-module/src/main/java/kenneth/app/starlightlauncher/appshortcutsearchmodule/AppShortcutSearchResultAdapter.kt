package kenneth.app.starlightlauncher.appshortcutsearchmodule

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.Rect
import android.os.Build
import android.os.Process
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.appshortcutsearchmodule.databinding.AppShortcutListBinding
import kenneth.app.starlightlauncher.appshortcutsearchmodule.databinding.AppShortcutListItemBinding

class AppShortcutSearchResultAdapter(
    private val context: Context,
    private val launcher: StarlightLauncherApi,
) : SearchResultAdapter {
    private val launcherApps =
        context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps

    private lateinit var parentContext: Context

    override fun onCreateViewHolder(parent: ViewGroup): SearchResultAdapter.ViewHolder {
        parentContext = parent.context
        val binding =
            AppShortcutListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                .apply {
                    appShortcutListBg.blurWith(launcher.blurHandler)
                }
        return AppShortcutSearchResultViewHolder(binding)
    }

    override fun onBindSearchResult(
        holder: SearchResultAdapter.ViewHolder,
        searchResult: SearchResult
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 &&
            holder is AppShortcutSearchResultViewHolder &&
            searchResult is AppShortcutSearchModule.Result
        ) {
            onBindSearchResult(holder, searchResult)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun onBindSearchResult(
        holder: AppShortcutSearchResultViewHolder,
        searchResult: AppShortcutSearchModule.Result
    ) {
        holder.binding.appShortcutList.removeAllViews()

        searchResult.shortcuts.forEach {
            AppShortcutListItemBinding.inflate(
                LayoutInflater.from(parentContext),
                holder.binding.appShortcutList,
                true
            )
                .apply {
                    appIcon = it.app?.getIcon(context.resources.displayMetrics.densityDpi)
                    appLabel = it.app?.label?.toString()
                    appShortcutIcon = launcherApps.getShortcutIconDrawable(
                        it.info,
                        context.resources.displayMetrics.densityDpi
                    )
                    appShortcutLabel =
                        it.info.longLabel?.toString() ?: it.info.shortLabel.toString()

                    root.setOnClickListener { _ -> openAppShortcut(this, it.info) }
                }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    private fun openAppShortcut(itemBinding: AppShortcutListItemBinding, shortcut: ShortcutInfo) {
        val sourceBound = Rect().apply {
            itemBinding.shortcutIconView.getGlobalVisibleRect(this)
        }

        launcherApps.startShortcut(
            shortcut.`package`,
            shortcut.id,
            sourceBound,
            null,
            Process.myUserHandle()
        )
    }
}

class AppShortcutSearchResultViewHolder(
    internal val binding: AppShortcutListBinding,
) : SearchResultAdapter.ViewHolder {
    override val rootView = binding.root
}