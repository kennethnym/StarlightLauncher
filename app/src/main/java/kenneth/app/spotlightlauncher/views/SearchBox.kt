package kenneth.app.spotlightlauncher.views

import android.animation.ValueAnimator
import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.PathInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.graphics.ColorUtils
import androidx.core.view.isInvisible
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppState
import kenneth.app.spotlightlauncher.databinding.SearchBoxBinding
import kenneth.app.spotlightlauncher.searching.ResultAdapter
import kenneth.app.spotlightlauncher.searching.Searcher
import kenneth.app.spotlightlauncher.utils.BindingRegister
import javax.inject.Inject

@AndroidEntryPoint
class SearchBox(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    /**
     * Whether the linear loading indicator under [SearchBox] should be visible
     */
    var shouldShowLoadingIndicator: Boolean
        get() = !binding.searchLoadingIndicator.isInvisible
        set(show) {
            binding.searchLoadingIndicator.isInvisible = !show
        }

    @Inject
    lateinit var searcher: Searcher

    @Inject
    lateinit var inputMethodManager: InputMethodManager

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var resultAdapter: ResultAdapter

    private val binding = SearchBoxBinding.inflate(LayoutInflater.from(context), this, true)

    private val searchBoxAnimationInterpolator by lazy {
        PathInterpolator(0.16f, 1f, 0.3f, 1f)
    }

    init {
        with(binding.searchBoxEditText) {
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) searcher.refreshAppList()
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

        binding.searchBoxContainer.setOnClickListener {
            binding.searchBoxEditText.requestFocus()
            inputMethodManager.toggleSoftInput(
                InputMethodManager.SHOW_FORCED,
                InputMethodManager.HIDE_IMPLICIT_ONLY,
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        binding.searchBoxBlurBackground.startBlur()
        binding.searchBoxEditText.setHintTextColor(
            ColorUtils.setAlphaComponent(
                appState.adaptiveTextColor,
                0x88
            )
        )
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        when (visibility) {
            View.GONE, View.INVISIBLE -> binding.searchBoxBlurBackground.pauseBlur()
            else -> binding.searchBoxBlurBackground.startBlur()
        }
    }

    override fun clearFocus() {
        super.clearFocus()
        binding.searchBoxEditText.clearFocus()
        inputMethodManager.hideSoftInputFromWindow(binding.searchBoxBlurBackground.windowToken, 0)
    }

    override fun isFocused(): Boolean {
        return binding.searchBoxEditText.isFocused
    }

    fun showTopPadding() {
        if (binding.searchBoxContainer.paddingTop <= 0) {
            createPaddingAnimation(showTopPadding = true).start()
        }
    }

    fun removeTopPadding() {
        if (binding.searchBoxContainer.paddingTop > 0) {
            createPaddingAnimation(showTopPadding = false).start()
        }
    }

    private fun handleSearchQuery(query: Editable?) {
        if (query == null || query.isBlank()) {
            searcher.cancelPendingSearch()
            resultAdapter.hideResult()
        } else {
            searcher.requestSearch(query.toString())
        }
    }

    private fun onSearchBoxFocusChanged(hasFocus: Boolean) {
        with(BindingRegister.activityMainBinding.widgetsPanel) {
            canBeSwiped = if (hasFocus) {
                expand()
                false
            } else {
                retract()
                true
            }
        }

        toggleSearchBoxAnimation(isActive = hasFocus)
    }

    private fun toggleSearchBoxAnimation(isActive: Boolean) {
        appState.isSearchBoxActive = isActive

        createPaddingAnimation(showTopPadding = isActive).start()

        with(BindingRegister.activityMainBinding.widgetsPanel) {
            if (isActive) {
                hideWidgets()
            } else {
                showWidgets()
            }
        }
    }

    private fun createPaddingAnimation(showTopPadding: Boolean): ValueAnimator {
        return ValueAnimator.ofInt(
            if (showTopPadding) 0 else appState.statusBarHeight,
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
    }
}