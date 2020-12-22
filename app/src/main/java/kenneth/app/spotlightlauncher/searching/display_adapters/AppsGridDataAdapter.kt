package kenneth.app.spotlightlauncher.searching.display_adapters

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ResolveInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter
import kenneth.app.spotlightlauncher.views.AppOptionMenu
import kenneth.app.spotlightlauncher.views.BlurView

/**
 * An adapter that displays apps in a grid.
 */
object AppsGridDataAdapter :
    RecyclerViewDataAdapter<ResolveInfo, AppsGridDataAdapter.ViewHolder>() {
    /**
     * The card container that is containing this RecyclerView
     */
    private lateinit var cardContainer: LinearLayout

    private lateinit var cardBlurBackground: BlurView

    override val layoutManager: GridLayoutManager
        get() = GridLayoutManager(activity, 5)

    override val recyclerView: RecyclerView
        get() = activity.findViewById(R.id.apps_grid)

    override fun getInstance(activity: Activity) = this.apply { this.activity = activity }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val gridItem = LayoutInflater.from(parent.context)
            .inflate(R.layout.apps_grid_item, parent, false) as LinearLayout

        return ViewHolder(gridItem, activity)
    }

    override fun displayData(data: List<ResolveInfo>?) {
        super.displayData(data)

        with(activity) {
            cardContainer = findViewById<LinearLayout>(R.id.apps_section_card).apply {
                visibility = View.VISIBLE
            }
            cardBlurBackground = findViewById<BlurView>(R.id.apps_section_card_blur_background)
                .also { it.startBlur() }
        }

        if (data?.isEmpty() != false) {
            with(activity) {
                findViewById<RecyclerView>(R.id.apps_grid).visibility = View.GONE
                findViewById<TextView>(R.id.apps_section_no_result).visibility = View.VISIBLE
            }
        } else {
            with(activity) {
                findViewById<RecyclerView>(R.id.apps_grid).visibility = View.VISIBLE
                findViewById<TextView>(R.id.apps_section_no_result).visibility = View.GONE
            }

            this.data = data
            notifyDataSetChanged()
        }
    }

    fun hideAppsGrid() {
        if (::cardContainer.isInitialized && ::cardBlurBackground.isInitialized) {
            cardBlurBackground.pauseBlur()
            cardContainer.visibility = View.GONE
        }
    }

    class ViewHolder(view: View, activity: Activity) :
        RecyclerViewDataAdapter.ViewHolder<ResolveInfo>(view, activity) {
        private lateinit var appOptionMenu: AppOptionMenu
        private lateinit var appInfo: ResolveInfo

        private val appearancePreferenceManager = AppearancePreferenceManager.getInstance(activity)
            .also {
                it.registerOnSharedPreferenceChangeListener(::onSharedPreferenceChanged)
            }

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

        private fun onSharedPreferenceChanged(
            @Suppress("UNUSED_PARAMETER") sharedPreferences: SharedPreferences,
            key: String
        ) {
            if (key == appearancePreferenceManager.showAppLabelsPrefKey) {
                setAppLabelVisibility()
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
                if (appearancePreferenceManager.showAppLabels) {
                    isVisible = true
                    text = appName
                } else {
                    isVisible = false
                }
            }
        }
    }
}
