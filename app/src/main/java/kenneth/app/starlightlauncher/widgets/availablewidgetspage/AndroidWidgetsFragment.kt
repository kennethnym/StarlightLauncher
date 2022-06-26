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
import kenneth.app.starlightlauncher.databinding.AvailableWidgetsPageBinding
import kenneth.app.starlightlauncher.databinding.FragmentAndroidWidgetListBinding
import kotlinx.coroutines.*

internal class AndroidWidgetsFragment(
    private val availableWidgetsPageBinding: AvailableWidgetsPageBinding,
    private val mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : Fragment() {
    private var listAdapter: AvailableAndroidWidgetListAdapter? = null

    private val appLabels = mutableMapOf<String, String>()
    private val appIcons = mutableMapOf<String, Drawable>()
    private val appInfos = mutableMapOf<String, ApplicationInfo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = context?.let { context ->
        FragmentAndroidWidgetListBinding.inflate(inflater).run {
            availableWidgetList.apply {
                clipToPadding = false
                setAdapter(AvailableAndroidWidgetListAdapter(context, this).also {
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

    private fun loadInstalledWidgets() {
        val context = context ?: return
        CoroutineScope(mainDispatcher).launch {
            val widgetProviders = withContext(ioDispatcher) {
                AppWidgetManager.getInstance(context)
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
            }
            listAdapter?.showAvailableWidgets(widgetProviders, appInfos, appLabels, appIcons)
        }
    }
}