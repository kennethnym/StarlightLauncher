package kenneth.app.starlightlauncher.views

import android.animation.ValueAnimator
import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.PathInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.AppState
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.databinding.SearchBoxBinding
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.util.BindingRegister
import javax.inject.Inject

@AndroidEntryPoint
internal class SearchBox(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    /**
     * Whether [SearchBox] contains search query.
     */
    val hasQueryText
        get() = !isQueryEmpty(binding.searchBoxEditText.text)

    @Inject
    lateinit var searcher: Searcher

    @Inject
    lateinit var inputMethodManager: InputMethodManager

    @Inject
    lateinit var blurHandler: BlurHandler

    @Inject
    lateinit var appState: AppState

    private val binding = SearchBoxBinding.inflate(LayoutInflater.from(context), this, true)

    private var isOpeningKeyboard = false

    private val searchBoxAnimationInterpolator by lazy {
        PathInterpolator(0.16f, 1f, 0.3f, 1f)
    }

    init {
        with(binding.searchBoxEditText) {
            setOnFocusChangeListener { _, hasFocus ->
                onSearchBoxFocusChanged(hasFocus)
            }

            setOnEditorActionListener { _, actionID, _ ->
                if (actionID == EditorInfo.IME_ACTION_DONE) {
                    clearFocus()
                }
                false
            }

            addTextChangedListener { text -> handleSearchQuery(text) }
        }

        with(binding) {
            searchBoxEditText.setOnClickListener {
                if (hasFocus()) {
                    isOpeningKeyboard = true
                    clearFocus()
                }
                requestFocus()
                inputMethodManager.showSoftInput(binding.searchBoxEditText, 0)
                isOpeningKeyboard = false
            }

            searchBoxContainer.setOnClickListener {
                if (binding.searchBoxEditText.hasFocus()) {
                    isOpeningKeyboard = true
                    binding.searchBoxEditText.clearFocus()
                }
                binding.searchBoxEditText.requestFocus()
                inputMethodManager.showSoftInput(binding.searchBoxEditText, 0)
                isOpeningKeyboard = false
            }

            searchBoxRightSideBtn.setOnClickListener { onRightSideButtonClicked() }

            searchBoxBg.blurWith(blurHandler)
        }
    }

    override fun clearFocus() {
        super.clearFocus()
        binding.searchBoxEditText.clearFocus()
        inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
    }

    override fun isFocused(): Boolean {
        return binding.searchBoxEditText.isFocused
    }

    fun unfocus() {
        binding.searchBoxEditText.clearFocus()
    }

    fun showTopPadding() {
        createPaddingAnimation(showTopPadding = true).start()
    }

    fun removeTopPadding() {
        createPaddingAnimation(showTopPadding = false).start()
    }

    fun showClearSearchBoxButton() {
        binding.searchBoxRightSideBtn.icon =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_times, context.theme)
    }

    fun showRetractWidgetPanelButton() {
        binding.searchBoxRightSideBtn.icon =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_angle_down, context.theme)
    }

    fun showExpandWidgetPanelButton() {
        binding.searchBoxRightSideBtn.icon =
            ResourcesCompat.getDrawable(resources, R.drawable.ic_angle_up, context.theme)
    }

    private fun handleSearchQuery(query: Editable?) {
        if (isQueryEmpty(query)) {
            searcher.cancelPendingSearch()
            BindingRegister.widgetsPanelBinding.searchResultView.clearSearchResults()
            BindingRegister.activityMainBinding.widgetsPanel.hideSearchResults()
        } else {
            showClearSearchBoxButton()
            searcher.requestSearch(query.toString())
        }
    }

    private fun onRightSideButtonClicked() {
        when {
            hasQueryText -> {
                binding.searchBoxEditText.text.clear()
                showRetractWidgetPanelButton()
            }
            BindingRegister.activityMainBinding.widgetsPanel.isExpanded -> {
                BindingRegister.activityMainBinding.widgetsPanel.retract()
                showExpandWidgetPanelButton()
            }
            else -> {
                BindingRegister.activityMainBinding.widgetsPanel.expand()
                showRetractWidgetPanelButton()
            }
        }
    }

    private fun onSearchBoxFocusChanged(hasFocus: Boolean) {
        if (!isOpeningKeyboard) {
            with(BindingRegister.activityMainBinding.widgetsPanel) {
                canBeSwiped = when {
                    hasFocus -> {
                        expand()
                        false
                    }
                    !hasQueryText -> {
                        retract()
                        true
                    }
                    else -> false
                }

                toggleSearchBoxAnimation(isActive = isExpanded)
            }
        }
    }

    private fun toggleSearchBoxAnimation(isActive: Boolean) {
        if (isActive && binding.searchBoxContainer.paddingTop < appState.statusBarHeight
            || !isActive && binding.searchBoxContainer.paddingTop > 0
        ) {
            createPaddingAnimation(showTopPadding = isActive).start()
        }

        with(BindingRegister.activityMainBinding.widgetsPanel) {
            if (isActive) {
                hideWidgets()
            } else {
                showWidgets()
            }
        }
    }

    private fun createPaddingAnimation(showTopPadding: Boolean): ValueAnimator =
        ValueAnimator.ofInt(
            paddingTop,
            if (showTopPadding) appState.statusBarHeight else 0,
        ).apply {
            interpolator = searchBoxAnimationInterpolator
            duration = 500

            addUpdateListener { updatedAnimation ->
                binding.searchBoxContainer.updatePadding(
                    top = updatedAnimation.animatedValue as Int
                )
            }
        }


    private fun isQueryEmpty(query: Editable?) = query == null || query.isBlank()
}