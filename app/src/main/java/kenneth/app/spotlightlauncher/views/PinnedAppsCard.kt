package kenneth.app.spotlightlauncher.views

import android.app.Activity
import android.content.Context
import android.content.pm.ResolveInfo
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.AppModule
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.prefs.PinnedAppsPreferenceManager
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.searching.AppSearcher
import kenneth.app.spotlightlauncher.searching.display_adapters.AppsGridDataAdapter
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.utils.activity
import java.lang.Exception
import javax.inject.Inject

@AndroidEntryPoint
class PinnedAppsCard(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs), LifecycleObserver {
    @Inject
    lateinit var pinnedAppsPreferenceManager: PinnedAppsPreferenceManager

    @Inject
    lateinit var appSearcher: AppSearcher

    private val pinnedApps: List<ResolveInfo>
        get() = appSearcher.apps
            .filter { pinnedAppsPreferenceManager.isAppPinned(it) }

    private lateinit var pinnedAppsRecyclerViewAdapter: PinnedAppsRecyclerViewAdapter

    init {
        inflate(context, R.layout.pinned_apps_card, this)

        isVisible = pinnedAppsPreferenceManager.pinnedApps.isNotEmpty()

        activity?.let {
            pinnedAppsRecyclerViewAdapter = PinnedAppsRecyclerViewAdapter.getInstance(it, this)
                .also { adapter ->
                    if (pinnedApps.isNotEmpty()) {
                        isVisible = true
                        adapter.displayData(pinnedApps)
                        it.lifecycle.addObserver(this)
                    } else {
                        isVisible = false
                        try {
                            it.lifecycle.removeObserver(this)
                        } catch (ex: Exception) {
                        }
                    }

                    pinnedAppsPreferenceManager.setPinnedAppsListener(::onPinnedAppsChanged)
                }
        }

        findViewById<BlurView>(R.id.pinned_apps_card_blur_background).startBlur()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun reloadLabels() {
        pinnedAppsRecyclerViewAdapter.reloadLabels()
    }

    private fun onPinnedAppsChanged() {
        if (!isVisible) {
            isVisible = true
        }
        pinnedAppsRecyclerViewAdapter.displayData(pinnedApps)
    }
}

object PinnedAppsRecyclerViewAdapter :
    RecyclerViewDataAdapter<ResolveInfo, AppsGridDataAdapter.ViewHolder>() {
    /**
     * The parent view that holds this RecyclerView.
     * This is required in order to obtain the instance of RecyclerView, because
     * it is freshly inflated, so calling findViewById on activity to find it will return null.
     */
    private lateinit var viewParent: View

    private lateinit var appearancePreferenceManager: AppearancePreferenceManager

    override val layoutManager: RecyclerView.LayoutManager
        get() = GridLayoutManager(activity, 5)

    override val recyclerView: RecyclerView
        get() = viewParent.findViewById(R.id.pinned_apps_grid)

    override fun getInstance(activity: Activity) = this.apply {
        this.activity = activity
        appearancePreferenceManager =
            AppModule.provideAppearancePreferenceManager(activity.applicationContext)
    }

    fun getInstance(activity: Activity, viewParent: View) = this.apply {
        this.activity = activity
        this.viewParent = viewParent
        appearancePreferenceManager =
            AppModule.provideAppearancePreferenceManager(activity.applicationContext)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AppsGridDataAdapter.ViewHolder {
        val gridItem = LayoutInflater.from(parent.context)
            .inflate(R.layout.apps_grid_item, parent, false) as LinearLayout

        return AppsGridDataAdapter.ViewHolder(gridItem, activity).apply {
            showLabel = appearancePreferenceManager.showNamesOfPinnedApps
        }
    }

    override fun displayData(data: List<ResolveInfo>?) {
        super.displayData(data)
        data?.let { this.data = it }
        notifyDataSetChanged()
    }

    fun reloadLabels() {
        for (i in 0 until itemCount) {
            recyclerView.getChildAt(i)?.let {
                ((recyclerView.getChildViewHolder(it)) as AppsGridDataAdapter.ViewHolder)
                    .showLabel = appearancePreferenceManager.showNamesOfPinnedApps
            }
        }

        notifyDataSetChanged()
    }
}
