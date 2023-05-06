package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.databinding.FragmentAndroidWidgetListBinding
import kenneth.app.starlightlauncher.databinding.FragmentAvailableWidgetsBinding
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

internal class AndroidWidgetsFragment @Inject constructor(
    private val launcher: StarlightLauncherApi,
    private val widgetsPreferenceManager: WidgetPreferenceManager,
    private val appWidgetHost: AppWidgetHost,
    private val appWidgetManager: AppWidgetManager,
) : Fragment(), AvailableAndroidWidgetListAdapter.Callback {
    private val requestBindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onRequestBindWidgetResult
    )

    private val configureWidgetActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onConfigureWidgetResult
    )

    private var binding: FragmentAndroidWidgetListBinding? = null
    private var listAdapter: AvailableAndroidWidgetListAdapter? = null

    private val appLabels = mutableMapOf<String, String>()
    private val appIcons = mutableMapOf<String, Drawable>()
    private val appInfos = mutableMapOf<String, ApplicationInfo>()

    var availableWidgetsPageBinding: FragmentAvailableWidgetsBinding? = null

    private val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            binding?.availableWidgetList?.updatePadding(
                bottom = availableWidgetsPageBinding?.tabBar?.height ?: 0,
            )
            binding?.root?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = context?.let { context ->
        FragmentAndroidWidgetListBinding.inflate(inflater).run {
            binding = this

            availableWidgetList.apply {
                clipToPadding = false
                setAdapter(
                    AvailableAndroidWidgetListAdapter(
                        context,
                        listView = this,
                        iconPack = runBlocking { launcher.iconPack.first() },
                        this@AndroidWidgetsFragment,
                    ).also {
                        listAdapter = it
                        loadInstalledWidgets()
                    })
                setGroupIndicator(null)
            }

            root.setOnApplyWindowInsetsListener { _, insets ->
                val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
                    .getInsets(WindowInsetsCompat.Type.systemBars())
                availableWidgetList.updatePadding(top = insetsCompat.top)
                insets
            }

            root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

            root
        }
    }

    override fun onRequestAddAndroidWidget(appWidgetInfo: AppWidgetProviderInfo) {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()
        bindAndroidWidget(appWidgetId, appWidgetInfo)
    }

    /**
     * Called when the user has answered the prompt to allow widgets to be displayed in Starlight.
     */
    private fun onRequestBindWidgetResult(result: ActivityResult?) {
        val data = result?.data ?: return
        val extras = data.extras
        val appWidgetId =
            extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: return
        val appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                configureAndroidWidget(appWidgetId, appWidgetProviderInfo)
            }

            Activity.RESULT_CANCELED -> {
                // user did not allow Starlight to show widgets
                // delete the widget
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }

    private fun onConfigureWidgetResult(result: ActivityResult?) {
        val data = result?.data ?: return
        val extras = data.extras
        val appWidgetId =
            extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) ?: return

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)
                if (appWidgetInfo != null) {
                    lifecycleScope.launch {
                        widgetsPreferenceManager.addAndroidWidget(appWidgetId, appWidgetInfo)
                        launcher.closeOverlay()
                    }
                } else {
                    appWidgetHost.deleteAppWidgetId(appWidgetId)
                }
            }

            Activity.RESULT_CANCELED -> {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
            }
        }
    }

    /**
     * Bind the given android widget with [AppWidgetManager].
     * Added android widgets must be bound before they can be displayed.
     */
    private fun bindAndroidWidget(
        appWidgetId: Int,
        appWidgetInfo: AppWidgetProviderInfo
    ) {
        // first, check if permission is granted to bind widgets
        val bindAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(
            appWidgetId,
            appWidgetInfo.provider,
        )

        if (bindAllowed) {
            Log.d("starlight", "bind allowed")
            // permission is granted, configure widget for display
            configureAndroidWidget(appWidgetId, appWidgetInfo)
        } else {
            // no permission, prompt user to allow widgets to be displayed in the launcher
            requestBindWidgetLauncher.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider)
            })
        }
    }

    private fun configureAndroidWidget(
        appWidgetId: Int,
        appWidgetInfo: AppWidgetProviderInfo
    ) {
        // check if the added widget has a configure activity
        // that lets user configure/customize the widget
        // before being added to the launcher
        if (appWidgetInfo.configure != null) {
            Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).run {
                component = appWidgetInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)

                configureWidgetActivityLauncher.launch(this)
            }
        } else {
            // no configuration required
            // refresh the widget list to show the newly added widget
            lifecycleScope.launch {
                widgetsPreferenceManager.addAndroidWidget(appWidgetId, appWidgetInfo)
                launcher.closeOverlay()
            }
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