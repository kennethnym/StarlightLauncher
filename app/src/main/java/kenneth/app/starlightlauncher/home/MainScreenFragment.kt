package kenneth.app.starlightlauncher.home

import android.appwidget.AppWidgetHost
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.text.Editable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
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
    private val searcher: Searcher,
    private val launcher: StarlightLauncherApi,
    private val launcherEventChannel: LauncherEventChannel,
    private val extensionManager: ExtensionManager,
    private val appWidgetHost: AppWidgetHost,
) : Fragment() {
    private var binding: FragmentMainScreenBinding? = null
    private val viewModel: MainScreenViewModel by viewModels()

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
            if (removedWidget is AddedWidget.AndroidWidget) {
                appWidgetHost.deleteAppWidgetId(removedWidget.appWidgetId)
            }
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

            mediaControlCard.setLifecycleScope(lifecycleScope)

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
            shouldMediaControlBeVisible.observe(viewLifecycleOwner) {
                toggleMediaControlCardVisibility(it)
            }
            activeMediaSession.observe(viewLifecycleOwner) {
                binding?.mediaControlCard?.mediaSession = it
            }
        }

        binding?.widgetsPanel?.widgetListView?.onWidgetListChangedListener =
            widgetListChangedListener

        searcher.addSearchResultListener { result, searchModule ->
            onSearchResultAvailable(result, searchModule)
        }

        lifecycleScope.launch {
            launcherEventChannel.subscribe {
                onLauncherEvent(it)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.recheckNotificationListener()
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

    private fun toggleMediaControlCardVisibility(isEnabled: Boolean) {
        binding?.mediaControlCard?.isVisible = isEnabled
        binding?.dateTimeViewContainer?.gravity =
            if (isEnabled) Gravity.CENTER or Gravity.BOTTOM
            else Gravity.CENTER
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
