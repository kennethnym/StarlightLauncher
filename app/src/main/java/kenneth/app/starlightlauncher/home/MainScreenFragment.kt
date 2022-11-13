package kenneth.app.starlightlauncher.home

import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.BindingRegister
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.LauncherEventChannel
import kenneth.app.starlightlauncher.api.LauncherEvent
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.databinding.FragmentMainScreenBinding
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.prefs.searching.SearchPreferenceChanged
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.views.LauncherOptionMenu
import kenneth.app.starlightlauncher.views.SearchBoxActionDelegate
import kenneth.app.starlightlauncher.widgets.AddedWidget
import kenneth.app.starlightlauncher.widgets.WidgetListView
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
internal class MainScreenFragment @Inject constructor(
    private val bindingRegister: BindingRegister,
    private val appWidgetManager: AppWidgetManager,
    private val appWidgetHost: AppWidgetHost,
    private val searcher: Searcher,
    private val launcher: StarlightLauncherApi,
    private val launcherEventChannel: LauncherEventChannel,
    private val extensionManager: ExtensionManager,
) : Fragment() {
    private var binding: FragmentMainScreenBinding? = null
    private val viewModel: MainScreenViewModel by viewModels()

    private val requestBindWidgetLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onRequestBindWidgetResult
    )

    private val configureWidgetActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ::onConfigureWidgetResult
    )

    /**
     * A BroadcastReceiver that receives broadcast of Intent.ACTION_TIME_TICK.
     */
    private val timeTickBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent?.action == Intent.ACTION_TIME_TICK) {
                updateDateTime(Calendar.getInstance().time)
            }
        }
    }

    private val widgetListChangedListener = object : WidgetListView.OnChangedListener {
        override fun onRequestRemoveWidget(removedWidget: AddedWidget) {
            viewModel.removeWidget(removedWidget)
        }

        override fun onWidgetResized(widget: AddedWidget, newHeight: Int) {
            viewModel.resizeWidget(widget, newHeight)
        }

        override fun onWidgetSwapped(oldPosition: Int, newPosition: Int) {
            viewModel.swapWidget(oldPosition, newPosition)
        }
    }

    private val searchBoxActionDelegate = object : SearchBoxActionDelegate {
        override fun retractWidgetsPanel() {
            binding?.run {
                widgetsPanel.retract()
                widgetsPanel.searchBox.isWidgetsPanelExpanded = false
            }
        }

        override fun expandWidgetsPanel() {
            binding?.run {
                widgetsPanel.expand()
                widgetsPanel.searchBox.isWidgetsPanelExpanded = true
            }
        }
    }

    override fun onAttach(context: Context) {
        context.registerReceiver(timeTickBroadcastReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        super.onAttach(context)
    }

    override fun onDestroy() {
        context?.unregisterReceiver(timeTickBroadcastReceiver)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = context?.let {
        FragmentMainScreenBinding.inflate(inflater).run {
            binding = this
            bindingRegister.mainScreenBinding = this

            dateTimeView.apply {
                // show the current time
                dateTime = Calendar.getInstance().time
                onRefreshWeatherRequested = {
                    viewModel.refreshWeather()
                }
            }

            widgetsPanel.searchBox.apply {
                actionDelegate = searchBoxActionDelegate
                onFocusChanged = View.OnFocusChangeListener { _, hasFocus ->
                    onSearchBoxFocusChanged(hasFocus)
                }

                addTextChangedListener {
                    handleSearchQuery(it)
                }
            }

            widgetsPanel.searchResultView.searchModules = extensionManager.installedSearchModules

            dateTimeViewContainer.setOnLongClickListener {
                launcher.showOptionMenu {
                    LauncherOptionMenu(
                        it.context,
                        launcher,
                        bindingRegister,
                        it
                    )
                }
                HANDLED
            }

            root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeWeatherInfo()

        with(viewModel) {
            clockSize.observe(viewLifecycleOwner) {
                binding?.dateTimeView?.clockSize = it
            }
            shouldUse24HrClock.observe(viewLifecycleOwner) {
                binding?.dateTimeView?.shouldUse24HrClock = it
            }
            addedWidgets.observe(viewLifecycleOwner) {
                binding?.widgetsPanel?.widgetListView?.widgets = it
            }
            searchResultOrder.observe(viewLifecycleOwner) {
                binding?.widgetsPanel?.searchResultView?.searchModuleOrder = it
            }
        }

        binding?.widgetsPanel?.widgetListView?.onWidgetListChangedListener =
            widgetListChangedListener

        viewModel.addedAndroidWidget.observe(viewLifecycleOwner) { (widget, appWidgetInfo) ->
            bindAndroidWidget(widget, appWidgetInfo)
        }

        searcher.addSearchResultListener { result, searchModule ->
            onSearchResultAvailable(result, searchModule)
        }

        lifecycleScope.launch {
            launcherEventChannel.subscribe {
                onLauncherEvent(it)
            }
        }
    }

    private fun onLauncherEvent(event: LauncherEvent) {
        when (event) {
            is SearchPreferenceChanged.SearchCategoryOrderChanged -> {
                binding?.widgetsPanel?.searchResultView?.swapPosition(
                    event.fromIndex,
                    event.toIndex
                )
            }
        }
    }

    private fun onSearchBoxFocusChanged(hasFocus: Boolean) {
        binding?.widgetsPanel?.canBeSwiped = when {
            hasFocus -> {
                binding?.widgetsPanel?.run {
                    expand()
                    hideWidgets()
                }
                false
            }

            binding?.widgetsPanel?.searchBox?.hasQueryText == false -> {
                binding?.widgetsPanel?.run {
                    retract()
                    showWidgetList()
                }
                true
            }

            else -> false
        }
    }

    private fun handleSearchQuery(query: Editable?) {
        if (query == null || query.isBlank()) {
            viewModel.cancelPendingSearch()
            binding?.widgetsPanel?.clearSearchResults()
        } else {
            binding?.widgetsPanel?.searchBox?.showClearSearchBoxButton()
            viewModel.requestSearch(query.toString())
        }
    }

    private fun onSearchResultAvailable(result: SearchResult, searchModule: SearchModule) {
        binding?.let { binding ->
            if (!binding.widgetsPanel.isSearchResultsVisible) {
                binding.widgetsPanel.showSearchResults()
            }
            binding.widgetsPanel.searchResultView.showSearchResult(result, searchModule)
        }
    }

    /**
     * Bind the given android widget with [AppWidgetManager].
     * Added android widgets must be bound before they can be displayed.
     */
    private fun bindAndroidWidget(
        widget: AddedWidget.AndroidWidget,
        appWidgetInfo: AppWidgetProviderInfo
    ) {
        // first, check if permission is granted to bind widgets
        val bindAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(
            widget.appWidgetId,
            widget.provider
        )

        if (bindAllowed) {
            Log.d("starlight", "bind allowed")
            // permission is granted, configure widget for display
            configureAndroidWidget(widget.appWidgetId, appWidgetInfo)
        } else {
            // no permission, prompt user to allow widgets to be displayed
            // in Starlight
            requestBindWidgetLauncher.launch(Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widget.appWidgetId)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider)
            })
        }
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
                viewModel.removeAndroidWidget(appWidgetId)
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
                viewModel.refreshWidgetList()
            }
            Activity.RESULT_CANCELED -> {
                appWidgetHost.deleteAppWidgetId(appWidgetId)
                viewModel.removeAndroidWidget(appWidgetId)
            }
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
            viewModel.refreshWidgetList()
        }
    }

    private fun observeWeatherInfo() {
        viewModel.weatherInfo.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                val (weatherUnit, weather) = data
                binding?.dateTimeView?.apply {
                    this.weatherUnit = weatherUnit
                    this.weather = weather
                }
            } else {
                binding?.dateTimeView?.isWeatherShown = false
            }
        }
    }

    private fun updateDateTime(date: Date) {
        binding?.dateTimeView?.dateTime = date
    }
}
