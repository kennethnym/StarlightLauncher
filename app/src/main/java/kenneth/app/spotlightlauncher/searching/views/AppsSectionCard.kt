package kenneth.app.spotlightlauncher.searching.views

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.GridLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.HANDLED
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.AppsGridItemBinding
import kenneth.app.spotlightlauncher.databinding.AppsSectionCardBinding
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.utils.BindingRegister
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.utils.activity
import kenneth.app.spotlightlauncher.views.SectionCard
import javax.inject.Inject
import kotlin.math.min

private const val APPS_GRID_ITEMS_PER_ROW = 5

/**
 * Defines how many apps are shown when [AppsSectionCard] is displayed initially.
 */
private const val INITIAL_ITEM_COUNT = 10

/**
 * Displays apps in the search result page
 */
@AndroidEntryPoint
class AppsSectionCard(context: Context, attrs: AttributeSet) :
    SectionCard<List<ResolveInfo>?>(context, attrs),
    LifecycleObserver {
    @Inject
    lateinit var appearancePreferenceManager: AppearancePreferenceManager

    @Inject
    lateinit var appsGridAdapter: AppsGridAdapter

    private val binding = AppsSectionCardBinding.inflate(LayoutInflater.from(context), this)

    /**
     * Stores the list of all apps to be shown by this card
     */
    private lateinit var allApps: List<ResolveInfo>

    init {
        title = context.getString(R.string.apps_section_title)
    }

    /**
     * Displays the given list of apps in this view.
     * @param apps The list of apps to be displayed. If null, this view will display a "not found" state.
     */
    override fun display(apps: List<ResolveInfo>?) {
        binding.appsGrid.apply {
            adapter = appsGridAdapter
            layoutManager = appsGridAdapter.layoutManager
        }

        if (apps?.isEmpty() != false) {
            with(binding) {
                appsGrid.isVisible = false
                showMoreButton.isVisible = false
                appsSectionNoResult.isVisible = true
            }
        } else {
            val totalAppCount = apps.size
            allApps = apps
            appsGridAdapter.data =
                if (totalAppCount > INITIAL_ITEM_COUNT)
                    apps.subList(0, INITIAL_ITEM_COUNT)
                else apps

            with(binding) {
                appsGrid.isVisible = true
                appsSectionNoResult.isVisible = false
            }

            with(binding.showMoreButton) {
                isVisible = allApps.size > INITIAL_ITEM_COUNT
                setOnClickListener { showMoreItems() }
            }

            activity?.lifecycle?.addObserver(this)
        }

        super.display(apps)
    }

    /**
     * Hides this card in the search result page.
     */
    override fun hide() {
        super.hide()
        activity?.lifecycle?.removeObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun reloadAppLabels() {
        for (i in 0 until appsGridAdapter.itemCount) {
            binding.appsGrid.getChildAt(i)?.let {
                (binding.appsGrid.getChildViewHolder(it) as AppsGridItem)
                    .isLabelShown = appearancePreferenceManager.areAppNamesInSearchResult

            }
        }
    }

    /**
     * Shows more apps in the apps grid
     */
    private fun showMoreItems() {
        // the total number of apps that can be displayed
        val totalItemCount = allApps.size
        // the current number of items in the grid
        val currentItemCount = appsGridAdapter.itemCount
        // the number of new apps to be added to the grid
        val addedItemsCount = min(INITIAL_ITEM_COUNT, totalItemCount - currentItemCount)
        // the total number of items after the items are added
        val newItemCount = currentItemCount + addedItemsCount

        appsGridAdapter.data += allApps.subList(
            currentItemCount,
            min(totalItemCount, newItemCount)
        )

        binding.showMoreButton.isVisible = newItemCount < totalItemCount

        appsGridAdapter.notifyItemRangeInserted(currentItemCount, addedItemsCount)
    }
}

class AppsGridAdapter @Inject constructor(
    @ActivityContext private val context: Context,
    private val appearanceManager: AppearancePreferenceManager,
) : RecyclerViewDataAdapter<ResolveInfo, AppsGridItem>() {
    override var data: List<ResolveInfo> = mutableListOf()

    override val layoutManager = GridLayoutManager(context, APPS_GRID_ITEMS_PER_ROW)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsGridItem {
        val binding =
            AppsGridItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        val isAppNameShown = appearanceManager.areAppNamesInSearchResult

        return AppsGridItem(binding, appearanceManager).apply {
            isLabelShown = isAppNameShown
        }
    }
}

/**
 * Creates a View in the apps grid that displays an app.
 * @param binding The view binding of the layout of [AppsGridItem]
 * @param appearancePreferenceManager The [AppearancePreferenceManager] to be used to generate icon of the app
 *                          this view is holding.
 */
class AppsGridItem(
    private val binding: AppsGridItemBinding,
    private val appearancePreferenceManager: AppearancePreferenceManager
) :
    RecyclerViewDataAdapter.ViewHolder<ResolveInfo>(binding.root) {
    /**
     * Whether this app item should show app name under the icon.
     */
    var isLabelShown: Boolean = true
        set(isLabelShown) {
            field = isLabelShown
            setAppLabelVisibility()
        }

    /**
     * The [ResolveInfo] of the app this view is showing.
     */
    private lateinit var appInfo: ResolveInfo

    private val packageManager = itemView.context.packageManager

    override fun bindWith(data: ResolveInfo) {
        appInfo = data

        val appName = appInfo.loadLabel(packageManager)
        val appIcon = appInfo.loadIcon(packageManager)

        binding.appIcon.apply {
            contentDescription = context.getString(R.string.app_icon_description, appName)

            appearancePreferenceManager.iconPack?.let {
                setImageBitmap(
                    it.getIconOf(
                        appInfo.activityInfo.packageName,
                        default = appIcon
                    )
                )
            } ?: setImageDrawable(appIcon)
        }

        with(binding.root) {
            setOnClickListener { openApp() }
            setOnLongClickListener {
                openAppOptionMenu()
                HANDLED
            }
        }

        setAppLabelVisibility()
    }

    /**
     * Launch the app when this view is clicked
     */
    private fun openApp() {
        val appActivity = appInfo.activityInfo
        val componentName =
            ComponentName(appActivity.applicationInfo.packageName, appActivity.name)

        val intent = Intent(Intent.ACTION_MAIN).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
            component = componentName
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        startActivity(itemView.context, intent, null)
    }

    /**
     * Open option menu for the app
     */
    private fun openAppOptionMenu() {
        BindingRegister.activityMainBinding.appOptionMenu.show(withApp = appInfo)
    }

    /**
     * Determines the visibility of app label based on whether it is enabled in shared pref
     */
    private fun setAppLabelVisibility() {
        if (::appInfo.isInitialized) {
            val appName = appInfo.loadLabel(packageManager)

            with(binding.appLabel) {
                if (isLabelShown) {
                    isVisible = true
                    text = appName
                } else {
                    isVisible = false
                }
            }
        }
    }
}