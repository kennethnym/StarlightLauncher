package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.widget.ExpandableListAdapter
import android.widget.LinearLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kenneth.app.starlightlauncher.databinding.AvailableWidgetsPageBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@EntryPoint
@InstallIn(SingletonComponent::class)
private interface AvailableWidgetsPageEntryPoint {
    fun availableWidgetsListAdapter(): AvailableWidgetsListAdapter
}

class AvailableWidgetsPage(context: Context) : LinearLayout(context) {
    private val binding = AvailableWidgetsPageBinding.inflate(LayoutInflater.from(context), this)

    private val appLabels = mutableMapOf<String, String>()
    private val appIcons = mutableMapOf<String, Drawable>()
    private val appInfos = mutableMapOf<String, ApplicationInfo>()

    private val hilt = EntryPointAccessors.fromApplication(
        context.applicationContext,
        AvailableWidgetsPageEntryPoint::class.java
    )

    private val availableWidgetsListAdapter: AvailableWidgetsListAdapter

    init {
        orientation = VERTICAL

        binding.availableWidgetList.apply {
            clipToPadding = false
            setAdapter(hilt.availableWidgetsListAdapter().also {
                availableWidgetsListAdapter = it
            })
            setGroupIndicator(null)
        }

        loadInstalledWidgets()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val insets = WindowInsetsCompat.toWindowInsetsCompat(rootWindowInsets)
            .getInsets(WindowInsetsCompat.Type.systemBars())
        binding.availableWidgetList.updatePadding(
            top = insets.top,
            bottom = insets.bottom,
        )
    }

    private fun loadInstalledWidgets() {
        CoroutineScope(Dispatchers.IO).launch {
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
                .run {
                    post {
                        availableWidgetsListAdapter.showAvailableWidgets(
                            this,
                            appInfos,
                            appLabels,
                            appIcons
                        )
                    }
                }
        }
    }
}