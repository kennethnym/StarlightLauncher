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
import androidx.core.graphics.ColorUtils
import androidx.core.view.isInvisible
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.AppState
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.databinding.SearchBoxBinding
import kenneth.app.starlightlauncher.prefs.appearance.DEFAULT_APP_DRAWER_ENABLED
import javax.inject.Inject

interface SearchBoxActionDelegate {
    fun retractWidgetsPanel()

    fun expandWidgetsPanel()

    fun openAppList()
}

@AndroidEntryPoint
internal class SearchBox(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    /**
     * Whether [SearchBox] contains search query.
     */
    val hasQueryText
        get() = !isQueryEmpty(binding.searchBoxEditText.text)

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

    var isWidgetsPanelExpanded: Boolean = false
        set(value) {
            field = value
            if (value) {
                showRetractWidgetPanelButton()
            } else {
                showExpandWidgetPanelButton()
            }
        }

    var isAllAppsButtonShown: Boolean = DEFAULT_APP_DRAWER_ENABLED
        set(isShown) {
            field = isShown
            binding.searchBoxLeftSideBtn.isInvisible = !isShown
        }

    /**
     * A delegate for handling action button click in the search box.
     */
    var actionDelegate: SearchBoxActionDelegate? = null

    var onFocusChanged: OnFocusChangeListener? = null
        set(value) {
            field = value
            binding.searchBoxEditText.onFocusChangeListener = value
        }

    init {
        with(binding.searchBoxEditText) {
            setOnEditorActionListener { _, actionID, _ ->
                if (actionID == EditorInfo.IME_ACTION_DONE) {
                    clearFocus()
                }
                false
            }
        }

        with(binding) {
            searchBoxEditText.setHintTextColor(
                ColorUtils.setAlphaComponent(
                    searchBoxEditText.currentTextColor,
                    0x80
                )
            )

            searchBoxEditText.setOnClickListener {
                requestFocus()
                inputMethodManager.showSoftInput(binding.searchBoxEditText, 0)
                isOpeningKeyboard = false
            }

            searchBoxRightSideBtn.setOnClickListener { onRightSideButtonClicked() }
            searchBoxLeftSideBtn.setOnClickListener { actionDelegate?.openAppList() }

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

    fun addTextChangedListener(afterTextChanged: (text: Editable?) -> Unit = {}) {
        binding.searchBoxEditText.addTextChangedListener {
            afterTextChanged(it)
        }
    }

    fun unfocus() {
        binding.searchBoxEditText.clearFocus()
    }

    /**
     * Clears the content of the search box.
     */
    fun clear() {
        binding.searchBoxEditText.text.clear()
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

    private fun onRightSideButtonClicked() {
        when {
            hasQueryText -> {
                clear()
                showRetractWidgetPanelButton()
            }

            isWidgetsPanelExpanded -> {
                actionDelegate?.retractWidgetsPanel()
                showExpandWidgetPanelButton()
            }

            else -> {
                actionDelegate?.expandWidgetsPanel()
                showRetractWidgetPanelButton()
            }
        }
    }

    private fun createPaddingAnimation(showTopPadding: Boolean): ValueAnimator =
        ValueAnimator.ofInt(
            binding.searchBoxContainer.paddingTop,
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