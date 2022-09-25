package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.appwidget.AppWidgetManager
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.FragmentComponent
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.databinding.AvailableWidgetsPageBinding
import kenneth.app.starlightlauncher.databinding.FragmentAndroidWidgetListBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@EntryPoint
@InstallIn(FragmentComponent::class)
private interface AndroidWidgetsFragmentEntryPoint {
    fun launcher(): StarlightLauncherApi
}

internal class AndroidWidgetsFragment(
    private val availableWidgetsPageBinding: AvailableWidgetsPageBinding,
) : Fragment() {
    private var listAdapter: AvailableAndroidWidgetListAdapter? = null
    private val launcher: StarlightLauncherApi

    private val appLabels = mutableMapOf<String, String>()
    private val appIcons = mutableMapOf<String, Drawable>()
    private val appInfos = mutableMapOf<String, ApplicationInfo>()

    init {
        EntryPointAccessors.fromFragment(this, AndroidWidgetsFragmentEntryPoint::class.java).run {
            launcher = launcher()
        }

        lifecycleScope.launch { listenToIconPack() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = context?.let { context ->
        FragmentAndroidWidgetListBinding.inflate(inflater).run {
            availableWidgetList.apply {
                clipToPadding = false
                setAdapter(
                    AvailableAndroidWidgetListAdapter(
                        context,
                        listView = this,
                        iconPack = runBlocking { launcher.iconPack.first() }
                    ).also {
                        listAdapter = it
                        loadInstalledWidgets()
                    })
                setGroupIndicator(null)
            }

            root.setOnApplyWindowInsetsListener { _, insets ->
                val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
                    .getInsets(WindowInsetsCompat.Type.systemBars())
                availableWidgetList.updatePadding(
                    top = insetsCompat.top,
                    bottom = availableWidgetsPageBinding.tabBar.height,
                )
                insets
            }

            root
        }
    }

    private suspend fun listenToIconPack() {
        launcher.iconPack
            .onEach { listAdapter?.changeIconPack(it) }
            .collect()
    }

    private fun loadInstalledWidgets() {
        val context = context ?: return
        lifecycleScope.launch {
            val widgetProviders = AppWidgetManager.getInstance(context)
                .installedProviders
                .groupBy {
                    val packageName = it.provider.packageName

                    val appInfo = context.packageManager.getApplicationInfo(
                        packageName,
                        PackageManager.GET_META_DATA
                    ).also { info -> appInfos[packageName] = info }

                    if (!appLabels.containsKey(packageName)) {
                        appLabels[packageName] =
                            appInfo.loadLabel(context.packageManager).toString()
                    }
                    if (!appIcons.containsKey(packageName)) {
                        appIcons[packageName] = appInfo.loadIcon(context.packageManager)
                    }

                    packageName
                }
                .toSortedMap { packageName1, packageName2 ->
                    appLabels[packageName1]!!.compareTo(appLabels[packageName2]!!)
                }

            listAdapter?.showAvailableWidgets(widgetProviders, appInfos, appLabels, appIcons)
        }
    }
}