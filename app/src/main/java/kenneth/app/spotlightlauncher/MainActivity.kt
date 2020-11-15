package kenneth.app.spotlightlauncher

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.transition.ChangeBounds
import android.transition.TransitionManager
import android.view.View
import android.view.animation.PathInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.graphics.ColorUtils
import androidx.core.widget.addTextChangedListener
import com.google.android.material.card.MaterialCardView
import kenneth.app.spotlightlauncher.prefs.SettingsActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private lateinit var rootView: ConstraintLayout
    private lateinit var searcher: Searcher
    private lateinit var resultAdapter: ResultAdapter
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

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

        rootView = findViewById<ConstraintLayout>(R.id.root).apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
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

        findViewById<Button>(R.id.temp_settings_button)
            .setOnClickListener { openSettings() }

        attachListeners()
    }

    private fun attachListeners() {
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

        rootView.setOnApplyWindowInsetsListener { _, insets ->
            statusBarHeight = insets.systemWindowInsetTop
            insets
        }

        searcher.setSearchResultListener { result, type ->
            runOnUiThread {
                resultAdapter.displayResult(result, type)
            }
        }
    }

    private fun openSettings() {
        val settingsIntent = Intent(this, SettingsActivity::class.java)
        startActivity(settingsIntent)
    }

    private fun askForPermission(permission: String) {
        requestedPermission = permission
        requestPermissionLauncher.launch(permission)
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

        if (isActive) {
            inactiveSearchBoxConstraints.clone(rootView)
            activeSearchBoxConstraints.clone(rootView)
            activeSearchBoxConstraints.clear(R.id.page, ConstraintSet.BOTTOM)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                rootView.systemUiVisibility =
                    rootView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }

            findViewById<LinearLayout>(R.id.search_box_container).apply {
                alpha = 1.0f
                setPadding(
                    searchBoxContainerPaddingPx,
                    searchBoxContainerPaddingPx + statusBarHeight,
                    searchBoxContainerPaddingPx,
                    searchBoxContainerPaddingPx
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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                rootView.systemUiVisibility =
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }

            findViewById<LinearLayout>(R.id.search_box_container).apply {
                alpha = 0.3f
                setPadding(
                    searchBoxContainerPaddingPx,
                    searchBoxContainerPaddingPx,
                    searchBoxContainerPaddingPx,
                    searchBoxContainerPaddingPx
                )
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

    private fun handleSearchQuery(query: Editable?) {
        if (query == null || query.isBlank()) {
            searcher.cancelPendingSearch()

            findViewById<MaterialCardView>(R.id.apps_section_card)
                .visibility = View.GONE
            findViewById<MaterialCardView>(R.id.files_section_card)
                .visibility = View.GONE
        } else {
            searcher.requestSearch(query.toString())
        }
    }
}
