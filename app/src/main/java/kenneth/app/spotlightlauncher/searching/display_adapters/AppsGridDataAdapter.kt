package kenneth.app.spotlightlauncher.searching.display_adapters

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ResolveInfo
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.spotlightlauncher.AppModule
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.views.AppOptionMenu
import kenneth.app.spotlightlauncher.views.BlurView
import kenneth.app.spotlightlauncher.views.TextButton
import kotlin.math.min

private const val INITIAL_ITEM_COUNT = 10

/**
 * An adapter that displays apps in a grid.
 */
object AppsGridDataAdapter :
    RecyclerViewDataAdapter<ResolveInfo, AppsGridDataAdapter.ViewHolder>(), LifecycleObserver {
    private lateinit var appearancePreferenceManager: AppearancePreferenceManager

    /**
     * The card container that is containing this RecyclerView
     */
    private lateinit var cardContainer: LinearLayout
    private lateinit var cardBlurBackground: BlurView

    /**
     * The TextView used to indicate there's no result available.
     */
    private lateinit var noResultLabel: TextView
    private lateinit var showMoreButton: TextButton

    /**
     * The data field only stores a portion of all the apps that came up in the search result
     * to avoid clutter and improve performance. This field is used to store the entire list of apps.
     */
    private lateinit var allData: List<ResolveInfo>

    override val layoutManager: GridLayoutManager
        get() = GridLayoutManager(activity, 5)

    override val recyclerView: RecyclerView
        get() = activity.findViewById(R.id.apps_grid)

    override fun getInstance(activity: Activity) = this.apply {
        this.activity = activity
        appearancePreferenceManager =
            AppModule.provideAppearancePreferenceManager(activity.applicationContext)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val gridItem = LayoutInflater.from(parent.context)
            .inflate(R.layout.apps_grid_item, parent, false) as LinearLayout

        return ViewHolder(gridItem, activity).apply {
            showLabel = appearancePreferenceManager.showAppNamesInSearchResult
        }
    }

    override fun displayData(data: List<ResolveInfo>?) {
        super.displayData(data)

        findViews()

        cardBlurBackground.isVisible = true
        cardContainer.isVisible = true

        if (data?.isEmpty() != false) {
            recyclerView.isVisible = false
            showMoreButton.isVisible = false
            noResultLabel.isVisible = true
            cardBlurBackground.pauseBlur()
            (activity as? AppCompatActivity).let {
                it?.lifecycle?.removeObserver(this)
            }
        } else {
            recyclerView.isVisible = true
            noResultLabel.isVisible = false
            cardBlurBackground.startBlur()

            with(showMoreButton) {
                isVisible = data.size > INITIAL_ITEM_COUNT
                setOnClickListener { showMoreItems() }
            }

            allData = data
            this.data = allData.subList(0, min(data.size, INITIAL_ITEM_COUNT))
                .toMutableList()

            notifyDataSetChanged()

            (activity as? AppCompatActivity).let {
                it?.lifecycle?.addObserver(this)
            }
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun onResume() {
        reloadAppLabels()
    }

    /**
     * Hides the views associated with this adapter.
     */
    fun hideAppsGrid() {
        (activity as? AppCompatActivity).let {
            it?.lifecycle?.removeObserver(this)
        }

        if (::cardContainer.isInitialized && ::cardBlurBackground.isInitialized) {
            cardBlurBackground.apply {
                pauseBlur()
                isVisible = false
            }
            cardContainer.isVisible = false
        }
    }

    /**
     * Finds and stores all relevant views
     */
    private fun findViews() {
        with(activity) {
            if (!::cardContainer.isInitialized) {
                cardContainer = findViewById(R.id.apps_section_card)
            }

            if (!::cardBlurBackground.isInitialized) {
                cardBlurBackground = findViewById(R.id.apps_section_card_blur_background)
            }
        }

        with(cardContainer) {
            if (!::noResultLabel.isInitialized) {
                noResultLabel = findViewById(R.id.apps_section_no_result)
            }

            if (!::showMoreButton.isInitialized) {
                showMoreButton = findViewById(R.id.show_more_button)
            }
        }
    }

    private fun reloadAppLabels() {
        for (i in 0 until itemCount) {
            recyclerView.getChildAt(i)?.let {
                (recyclerView.getChildViewHolder(it) as ViewHolder)
                    .showLabel = appearancePreferenceManager.showAppNamesInSearchResult

            }
        }

        notifyDataSetChanged()
    }

    private fun showMoreItems() {
        val currentItemCount = data.size
        val newItemCount = currentItemCount + INITIAL_ITEM_COUNT
        val totalItemCount = allData.size

        (data as MutableList).addAll(
            allData.subList(
                currentItemCount,
                min(totalItemCount, newItemCount)
            )
        )
        showMoreButton.isVisible = newItemCount < totalItemCount

        notifyItemRangeInserted(currentItemCount, INITIAL_ITEM_COUNT)
    }

    class ViewHolder(view: View, activity: Activity) :
        RecyclerViewDataAdapter.ViewHolder<ResolveInfo>(view, activity) {
        /**
         * Whether this app item should show app name under the icon.
         */
        var showLabel: Boolean = true

        private lateinit var appOptionMenu: AppOptionMenu
        private lateinit var appInfo: ResolveInfo

        private val appearancePreferenceManager = AppearancePreferenceManager.getInstance(activity)

        override fun bindWith(data: ResolveInfo) {
            val appInfo = data

            this.appInfo = appInfo

            val appName = appInfo.loadLabel(activity.packageManager)
            val appIcon = appInfo.loadIcon(activity.packageManager)

            appOptionMenu = activity.findViewById(R.id.app_option_menu)

            with(view) {
                findViewById<ImageView>(R.id.app_icon).apply {
                    contentDescription = activity.getString(R.string.app_icon_description, appName)
                    appearancePreferenceManager.iconPack?.let {
                        setImageBitmap(
                            it.getIconOf(
                                appInfo.activityInfo.packageName,
                                default = appIcon
                            )
                        )
                    } ?: setImageDrawable(appIcon)
                }

                setAppLabelVisibility()

                setOnClickListener { openApp() }

                setOnLongClickListener {
                    openAppOptionMenu()
                    true
                }
            }
        }

        /**
         * Launch the clicked app
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

            activity.startActivity(intent)
        }

        /**
         * Open option menu for the app
         */
        private fun openAppOptionMenu() {
            appOptionMenu.show(withApp = appInfo)
        }

        /**
         * Determines the visibility of app label based on whether it is enabled in shared pref
         */
        private fun setAppLabelVisibility() {
            val appName = appInfo.loadLabel(activity.packageManager)

            with(view.findViewById<TextView>(R.id.app_label)) {
                if (showLabel) {
                    isVisible = true
                    text = appName
                } else {
                    isVisible = false
                }
            }
        }
    }
}
