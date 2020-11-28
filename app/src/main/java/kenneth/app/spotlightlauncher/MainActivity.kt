package kenneth.app.spotlightlauncher

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.WindowInsetsController
import android.view.animation.PathInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.ColorUtils
import androidx.core.view.updatePadding
import androidx.core.widget.addTextChangedListener
import com.google.android.material.card.MaterialCardView
import kenneth.app.spotlightlauncher.prefs.SettingsActivity
import kenneth.app.spotlightlauncher.searching.SearchType
import kenneth.app.spotlightlauncher.searching.Searcher
import kenneth.app.spotlightlauncher.searching.display_adapters.ResultAdapter
import kenneth.app.spotlightlauncher.utils.KeyboardAnimationCallback
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var rootView: ConstraintLayout
    private lateinit var searcher: Searcher
    private lateinit var resultAdapter: ResultAdapter
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var keyboardAnimationCallback: KeyboardAnimationCallback
    private lateinit var sectionCardList: LinearLayout
    private lateinit var searchProgressBar: ProgressBar

    private var isSearchScreenActive = false
    private var isDarkModeActive = false
    private var searchBoxContainerPaddingPx: Int = 0
    private var statusBarHeight: Int = 0

    /**
     * Right before requesting a permission, it is stored in this variable, so that when
     * the request result comes back, we know what permission is being requested, and we
     * can hide the corresponding button.
     */
    private var requestedPermission: String = ""

    private lateinit var searchBox: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // enable edge-to-edge app experience

        rootView = findViewById(R.id.root)
        sectionCardList = findViewById(R.id.section_card_list)
        searchProgressBar = findViewById(R.id.search_progress_bar)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            rootView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        } else {
            window.setDecorFitsSystemWindows(false)
        }

        resultAdapter = ResultAdapter(this)

        searcher = Searcher(packageManager, applicationContext)
        searchBoxContainerPaddingPx =
            resources.getDimensionPixelSize(R.dimen.search_box_container_padding)

        requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission(),
                ::handlePermissionResult
            )

        isDarkModeActive =
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) resources.configuration.isNightModeActive
            else resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        attachListeners()
    }

    override fun onBackPressed() {
        if (searchBox.text.toString() == "") {
            toggleSearchBoxAnimation(isActive = false)
            searchBox.clearFocus()
        } else {
            super.onBackPressed()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        isDarkModeActive =
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) newConfig.isNightModeActive
            else newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        if (isSearchScreenActive) {
            if (isDarkModeActive) {
                disableLightStatusBar()
            } else {
                enableLightStatusBar()
            }
        }
    }

    private fun attachListeners() {
        findViewById<Button>(R.id.temp_settings_button)
            .setOnClickListener { openSettings() }

        searchBox = findViewById<EditText>(R.id.search_box).also {
            it.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) searcher.refreshAppList()
                onSearchBoxFocusChanged(hasFocus)
            }

            it.setOnEditorActionListener { _, actionID, _ ->
                if (actionID == EditorInfo.IME_ACTION_DONE) {
                    it.clearFocus()
                }
                false
            }

            it.addTextChangedListener { text -> handleSearchQuery(text) }
        }

        findViewById<Button>(R.id.open_settings_button)
            .setOnClickListener { openSettings() }

        with(rootView) {
            setOnApplyWindowInsetsListener { _, insets ->
                statusBarHeight = insets.systemWindowInsetTop
                insets
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            keyboardAnimationCallback = KeyboardAnimationCallback(rootView).also {
                rootView.setWindowInsetsAnimationCallback(it)
            }
        }

        with(searcher) {
            setSearchResultListener { result, type ->
                runOnUiThread {
                    resultAdapter.displayResult(result, type)
                }
            }

            setWebResultListener { result ->
                runOnUiThread {
                    searchProgressBar.visibility = View.GONE
                    resultAdapter.displayWebResult(result)
                }
            }
        }
    }

    private fun openSettings() {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        if (!isGranted) return

        val query = findViewById<EditText>(R.id.search_box).text.toString()

        when (requestedPermission) {
            Manifest.permission.READ_EXTERNAL_STORAGE -> searcher.requestSpecificSearch(
                SearchType.FILES,
                query
            )
        }
    }

    private fun onSearchBoxFocusChanged(hasFocus: Boolean) {
        if (searchBox.text.isBlank()) {
            toggleSearchBoxAnimation(hasFocus)
        }
    }

    private fun toggleSearchBoxAnimation(isActive: Boolean) {
        val inactiveSearchBoxConstraints = ConstraintSet()
        val activeSearchBoxConstraints = ConstraintSet()

        isSearchScreenActive = isActive

        if (isActive) {
            inactiveSearchBoxConstraints.clone(rootView)
            activeSearchBoxConstraints.clone(rootView)
            activeSearchBoxConstraints.clear(R.id.page, ConstraintSet.BOTTOM)

            if (!isDarkModeActive) {
                enableLightStatusBar()
            }

            findViewById<LinearLayout>(R.id.search_box_container).apply {
                alpha = 1.0f
                updatePadding(
                    top = searchBoxContainerPaddingPx + statusBarHeight,
                )
            }

            findViewById<ConstraintLayout>(R.id.root)
                .setBackgroundColor(
                    ColorUtils.setAlphaComponent(android.R.attr.colorBackground, 128)
                )
        } else {
            inactiveSearchBoxConstraints.clone(rootView)
            activeSearchBoxConstraints.clone(rootView)
            inactiveSearchBoxConstraints.connect(
                R.id.page,
                ConstraintSet.BOTTOM,
                R.id.root,
                ConstraintSet.BOTTOM
            )

            disableLightStatusBar()

            findViewById<LinearLayout>(R.id.search_box_container).apply {
                alpha = 0.3f
                updatePadding(top = 0)
            }

            findViewById<ConstraintLayout>(R.id.root)
                .setBackgroundColor(
                    ColorUtils.setAlphaComponent(android.R.attr.colorBackground, 0)
                )
        }

        val transition = ChangeBounds().apply {
            duration = 500L
            interpolator = PathInterpolator(0.16f, 1f, 0.3f, 1f)
        }

        TransitionManager.beginDelayedTransition(rootView, transition)

        val constraints =
            if (isActive) activeSearchBoxConstraints
            else inactiveSearchBoxConstraints

        constraints.applyTo(root)
    }

    @SuppressLint("InlinedApi")
    private fun enableLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window
                .insetsController
                ?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
        } else if (Build.VERSION.SDK_INT in (Build.VERSION_CODES.M..Build.VERSION_CODES.Q) && !isDarkModeActive) {
            rootView.systemUiVisibility =
                rootView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun disableLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window
                .insetsController
                ?.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
        } else if (Build.VERSION.SDK_INT in (Build.VERSION_CODES.M..Build.VERSION_CODES.Q) && !isDarkModeActive) {
            rootView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
    }

    private fun handleSearchQuery(query: Editable?) {
        if (query == null || query.isBlank()) {
            searchProgressBar.visibility = View.GONE
            searcher.cancelPendingSearch()
            hideAllCards()
        } else {
            searchProgressBar.visibility = View.VISIBLE
            searcher.requestSearch(query.toString())
        }
    }

    private fun hideAllCards() {
        findViewById<MaterialCardView>(R.id.apps_section_card)
            .visibility = View.GONE
        findViewById<MaterialCardView>(R.id.files_section_card)
            .visibility = View.GONE
        findViewById<MaterialCardView>(R.id.suggested_section_card)
            .visibility = View.GONE

        sectionCardList.removeView(findViewById(R.id.web_result_section_card))
    }
}
