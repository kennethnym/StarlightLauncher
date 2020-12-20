package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.content.pm.ResolveInfo
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.prefs.PinnedAppsPreferenceManager
import kenneth.app.spotlightlauncher.utils.dp
import kenneth.app.spotlightlauncher.utils.navBarHeight
import javax.inject.Inject

@AndroidEntryPoint
class AppOptionMenu(context: Context, attrs: AttributeSet) : BottomOptionMenu(context, attrs) {
    @Inject
    lateinit var pinnedAppsPreferenceManager: PinnedAppsPreferenceManager

    private lateinit var app: ResolveInfo

    private val appIcon: ImageView
    private val appLabel: TextView

    // menu items
    private val pinAppItem: Item

    init {
        inflate(context, R.layout.app_option_menu, this)

        appLabel = findViewById(R.id.app_option_menu_app_label)
        appIcon = findViewById(R.id.app_option_menu_app_icon)
        pinAppItem = findViewById(R.id.pin_app_item)

        attachListeners()
    }

    fun show(withApp: ResolveInfo) {
        app = withApp
        val appName = app.loadLabel(context.packageManager)

        appIcon.apply {
            contentDescription = context.getString(R.string.app_icon_description, appName)
            setImageDrawable(app.loadIcon(context.packageManager))
        }
        appLabel.text = appName

        isVisible = true
        showIsAppPinned(pinnedAppsPreferenceManager.isAppPinned(app))

        super.show()
    }

    private fun attachListeners() {
        setOnClickListener { hide() }
        pinAppItem.setOnClickListener { togglePin() }
    }

    private fun togglePin() {
        val pinnedApps = pinnedAppsPreferenceManager.pinnedApps

        if (pinnedApps.isEmpty() || !pinnedAppsPreferenceManager.isAppPinned(app)) {
            pinnedAppsPreferenceManager.addPinnedApps(app)
            showIsAppPinned(true)
        } else {
            pinnedAppsPreferenceManager.removePinnedApps(app)
            showIsAppPinned(false)
        }
    }

    private fun showIsAppPinned(isPinned: Boolean) {
        if (isPinned) {
            pinAppItem.apply {
                context.getDrawable(R.drawable.ic_times_circle)
                    ?.also { setIcon(it) }
                setLabel(context.getString(R.string.unpin_app_label))
            }
        } else {
            pinAppItem.apply {
                context.getDrawable(R.drawable.ic_favorite)
                    ?.also { setIcon(it) }
                setLabel(context.getString(R.string.pin_app_label))
            }
        }
    }
}