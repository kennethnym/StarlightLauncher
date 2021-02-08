package kenneth.app.spotlightlauncher.widgets

import android.content.Context
import android.content.pm.ResolveInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.databinding.AppsGridItemBinding
import kenneth.app.spotlightlauncher.databinding.PinnedAppsCardBinding
import kenneth.app.spotlightlauncher.prefs.PinnedAppsPreferenceManager
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.searching.AppSearcher
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
    lateinit var appSearcher: AppSearcher

    private val binding = PinnedAppsCardBinding.inflate(LayoutInflater.from(context), this, true)

    private val pinnedApps: List<ResolveInfo>
        get() = appSearcher.apps
            .filter { pinnedAppsPreferenceManager.isAppPinned(it) }

    init {
        isVisible = pinnedAppsPreferenceManager.pinnedApps.isNotEmpty()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        changeCardVisibility()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        reloadAppLabels()
    }

    private fun reloadAppLabels() {
        for (i in 0 until pinnedAppsGridAdapter.itemCount) {
            binding.pinnedAppsGrid.getChildAt(i)?.let {
                (binding.pinnedAppsGrid.getChildViewHolder(it) as AppsGridItem)
                    .isLabelShown = appearancePreferenceManager.areNamesOfPinnedAppsShown

            }
        }
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
            pinnedAppsPreferenceManager.setPinnedAppsListener(::onPinnedAppsChanged)
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
) :
    RecyclerViewDataAdapter<ResolveInfo, AppsGridItem>() {
    override val layoutManager: RecyclerView.LayoutManager
        get() = GridLayoutManager(context, 5)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppsGridItem {
        val binding =
            AppsGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val isAppNameShown = appearancePreferenceManager.areNamesOfPinnedAppsShown

        return AppsGridItem(binding, appearancePreferenceManager).apply {
            isLabelShown = isAppNameShown
        }
    }
}
