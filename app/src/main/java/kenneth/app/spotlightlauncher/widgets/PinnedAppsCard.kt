package kenneth.app.spotlightlauncher.widgets

import android.content.*
import android.content.pm.ResolveInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleObserver
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.databinding.AppsGridItemBinding
import kenneth.app.spotlightlauncher.databinding.PinnedAppsCardBinding
import kenneth.app.spotlightlauncher.prefs.PinnedAppsPreferenceManager
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.searching.AppManager
import kenneth.app.spotlightlauncher.searching.views.AppsGridItem
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.utils.activity
import javax.inject.Inject

@AndroidEntryPoint
class PinnedAppsCard(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs), LifecycleObserver {
    @Inject
    lateinit var pinnedAppsPreferenceManager: PinnedAppsPreferenceManager

    @Inject
    lateinit var appearancePreferenceManager: AppearancePreferenceManager

    @Inject
    lateinit var pinnedAppsGridAdapter: PinnedAppsGridAdapter

    @Inject
    lateinit var appManager: AppManager

    private val binding = PinnedAppsCardBinding.inflate(LayoutInflater.from(context), this, true)

    private val pinnedApps: List<ResolveInfo>
        get() = appManager.apps
            .filter { pinnedAppsPreferenceManager.isAppPinned(it) }

    init {
        isVisible = pinnedAppsPreferenceManager.pinnedApps.isNotEmpty()
        pinnedAppsPreferenceManager.setPinnedAppsListener(::onPinnedAppsChanged)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        changeCardVisibility()
    }

    /**
     * Changes the visibility of [PinnedAppsCard] based on whether there is any pinned app.
     */
    private fun changeCardVisibility() {
        val pinnedApps = this.pinnedApps
        if (pinnedApps.isNotEmpty()) {
            isVisible = true
            binding.pinnedAppsGrid.apply {
                adapter = pinnedAppsGridAdapter.apply {
                    data = pinnedApps
                }
                layoutManager = pinnedAppsGridAdapter.layoutManager
            }
            activity?.lifecycle?.addObserver(this)
            binding.pinnedAppsCardBlurBackground.startBlur()
        } else {
            isVisible = false
            activity?.lifecycle?.removeObserver(this)
            binding.pinnedAppsCardBlurBackground.pauseBlur()
        }
    }

    private fun onPinnedAppsChanged() {
        changeCardVisibility()
    }
}

class PinnedAppsGridAdapter @Inject constructor(
    @ActivityContext private val context: Context,
    private val appearancePreferenceManager: AppearancePreferenceManager,
    private val inputMethodManager: InputMethodManager
) :
    RecyclerViewDataAdapter<ResolveInfo, AppsGridItem>() {
    override var data = listOf<ResolveInfo>()

    override val layoutManager: RecyclerView.LayoutManager
        get() = GridLayoutManager(context, 5)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppsGridItem {
        val binding =
            AppsGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return PinnedAppsGridItem(binding, appearancePreferenceManager, inputMethodManager)
    }
}

/**
 * Extends [AppsGridItem] which is shown in the apps section of search result since
 * its behavior is basically the same as grid item in pinned apps grid.
 */
class PinnedAppsGridItem(
    binding: AppsGridItemBinding,
    private val appearancePreferenceManager: AppearancePreferenceManager,
    inputMethodManager: InputMethodManager
) : AppsGridItem(binding, appearancePreferenceManager, inputMethodManager) {
    override val isLabelShown: Boolean
        get() = appearancePreferenceManager.areNamesOfPinnedAppsShown

    override fun onAppearancePreferencesChanged(key: String) {
        if (key == AppearancePreferenceManager.showPinnedAppsLabelsKey) {
            setAppLabelVisibility()
        } else {
            super.onAppearancePreferencesChanged(key)
        }
    }
}
