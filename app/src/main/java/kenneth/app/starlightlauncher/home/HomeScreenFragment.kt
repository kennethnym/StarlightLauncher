package kenneth.app.starlightlauncher.home

import android.appwidget.AppWidgetHost
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.BackEventCompat
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.ANIMATION_FRAME_DELAY
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.LauncherEventChannel
import kenneth.app.starlightlauncher.LauncherState
import kenneth.app.starlightlauncher.NightModeChanged
import kenneth.app.starlightlauncher.animation.TranslationYPredictiveOnBackPressedCallback
import kenneth.app.starlightlauncher.api.LauncherEvent
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.databinding.FragmentMainScreenBinding
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.prefs.StarlightLauncherSettingsActivity
import kenneth.app.starlightlauncher.prefs.searching.SearchPreferenceChanged
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.views.LauncherOptionMenu
import kenneth.app.starlightlauncher.views.SearchBoxActionDelegate
import kenneth.app.starlightlauncher.widgets.AddedWidget
import kenneth.app.starlightlauncher.widgets.WidgetListView
import kenneth.app.starlightlauncher.widgets.availablewidgetspage.AvailableWidgetsFragment
import kenneth.app.starlightlauncher.widgets.widgetspanel.WidgetsPanel
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
internal class HomeScreenFragment @Inject constructor(
    private val searcher: Searcher,
    private val launcher: StarlightLauncherApi,
    private val launcherEventChannel: LauncherEventChannel,
    private val extensionManager: ExtensionManager,
    private val appWidgetHost: AppWidgetHost,
    private val launcherState: LauncherState,
) : Fragment() {
    lateinit var homeScreenViewPager: ViewPager2

    private var binding: FragmentMainScreenBinding? = null
    private val viewModel: HomeScreenViewModel by viewModels()

    private var isDateTimeScaleEffectEnabled = false

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

        override fun onWidgetReordered(newList: List<AddedWidget>) {
            viewModel.updateWidgetList(newList)
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

        override fun openAppList() {
            homeScreenViewPager.currentItem = POSITION_HOME_SCREEN_VIEW_PAGER_APP_DRAWER
        }
    }

    private val launcherOptionMenuDelegate = object : LauncherOptionMenu.Delegate {
        override fun openSettings() {
            startActivity(
                Intent(context, StarlightLauncherSettingsActivity::class.java)
            )
        }

        override fun openWidgetSelectorOverlay() {
            launcher.showOverlay(AvailableWidgetsFragment())
        }

        override fun enableWidgetEditMode() {
            binding?.widgetsPanel?.enterEditMode()
        }
    }

    private val widgetsPaneListener = object : WidgetsPanel.Listener {
        override fun onExpanded() {
            if (!widgetsPanelExpandedBackCallback.isAdded) {
                activity?.onBackPressedDispatcher?.addCallback(widgetsPanelExpandedBackCallback)
                widgetsPanelExpandedBackCallback.isAdded = true
            }
        }

        override fun onRetracted() {
            widgetsPanelExpandedBackCallback.apply {
                remove()
                isAdded = false
            }
        }
    }

    /**
     * An on back pressed callback that retracts the widget panel when the back button is pressed
     */
    private val widgetsPanelExpandedBackCallback by lazy {
        object : TranslationYPredictiveOnBackPressedCallback(binding!!.widgetsPanel, true) {
            var isAdded = false

            override fun handleOnBackPressed() {
                binding?.widgetsPanel?.retract()
            }
        }
    }

    /**
     * An on back pressed callback that clears the search results when the back button is triggered
     */
    private val searchResultsBackCallback by lazy {
        object : OnBackPressedCallback(true) {
            var isAdded = false

            private var beginningAlpha = 0f
            private var beginningTranslationX = 0f

            override fun handleOnBackPressed() {
                val binding = this@HomeScreenFragment.binding ?: return
                binding.widgetsPanel.searchBox.clear()
                binding.widgetsPanel.searchResultView.apply {
                    alpha = beginningAlpha
                    translationX = beginningTranslationX
                }
            }

            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun handleOnBackStarted(backEvent: BackEventCompat) {
                val binding = this@HomeScreenFragment.binding ?: return
                beginningAlpha = binding.widgetsPanel.searchResultView.alpha
                beginningTranslationX = binding.widgetsPanel.searchResultView.translationX
            }

            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun handleOnBackProgressed(backEvent: BackEventCompat) {
                binding?.widgetsPanel?.searchResultView?.apply {
                    alpha = beginningAlpha * (1 - backEvent.progress)
                    translationX = beginningTranslationX + (500 * backEvent.progress)
                }
            }

            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun handleOnBackCancelled() {
                binding?.widgetsPanel?.searchResultView?.apply {
                    alpha = beginningAlpha
                    translationX = beginningTranslationX
                }
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
                    LauncherOptionMenu(it, launcherOptionMenuDelegate)
                }
                HANDLED
            }

            widgetsPanel.listener = widgetsPaneListener

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
            isAllAppsScreenEnabled.observe(viewLifecycleOwner) {
                binding?.widgetsPanel?.searchBox?.isAllAppsButtonShown = it
            }
        }

        binding?.widgetsPanel?.widgetListView?.onWidgetListChangedListener =
            widgetListChangedListener

        enableDateTimeViewScaleEffect()

        searcher.addSearchResultListener { result, searchModule ->
            onSearchResultAvailable(result, searchModule)
        }

        lifecycleScope.launch {
            launcherEventChannel.subscribe {
                onLauncherEvent(it)
            }
        }
    }

    override fun onDestroyView() {
        disableDateTimeViewScaleEffect()
        super.onDestroyView()
    }

    override fun onPause() {
        disableDateTimeViewScaleEffect()
        super.onPause()
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

            is NightModeChanged -> {
                binding?.widgetsPanel?.redrawAndroidWidgets()
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
        if (query.isNullOrBlank()) {
            searchResultsBackCallback.apply {
                remove()
                isAdded = false
            }
            viewModel.cancelPendingSearch()
            binding?.widgetsPanel?.clearSearchResults()
        } else {
            if (!searchResultsBackCallback.isAdded) {
                searchResultsBackCallback.isAdded = true
                activity?.onBackPressedDispatcher?.addCallback(searchResultsBackCallback)
            }
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

    private fun enableDateTimeViewScaleEffect() {
        if (!isDateTimeScaleEffectEnabled) {
            isDateTimeScaleEffectEnabled = true
            Choreographer.getInstance()
                .postFrameCallbackDelayed(::updateDateTimeViewScale, ANIMATION_FRAME_DELAY)
        }
    }

    private fun disableDateTimeViewScaleEffect() {
        Choreographer.getInstance().removeFrameCallback(::updateDateTimeViewScale)
        isDateTimeScaleEffectEnabled = false
    }

    private fun updateDateTimeViewScale(delay: Long) {
        val binding = this.binding ?: return
        val newScale = max(
            0f,
            (binding.dateTimeView.y - binding.widgetsPanel.y) / (binding.dateTimeView.y - launcherState.halfScreenHeight)
        )

        binding.dateTimeViewContainer.apply {
            scaleX = newScale
            scaleY = newScale
        }

        Choreographer.getInstance()
            .postFrameCallbackDelayed(::updateDateTimeViewScale, ANIMATION_FRAME_DELAY)
    }
}
