package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.content.pm.ResolveInfo
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.AppOptionMenuBinding
import kenneth.app.spotlightlauncher.prefs.PinnedAppsPreferenceManager
import javax.inject.Inject

@AndroidEntryPoint
class AppOptionMenu(context: Context, attrs: AttributeSet) : BottomOptionMenu(context, attrs) {
    @Inject
    lateinit var pinnedAppsPreferenceManager: PinnedAppsPreferenceManager

    private lateinit var app: ResolveInfo

    private val binding: AppOptionMenuBinding =
        AppOptionMenuBinding.inflate(LayoutInflater.from(context), this)

    private val appLabel: TextView
    private val appIcon: ImageView

    // menu items
    private val pinAppItem: Item

    init {
        appLabel = binding.appOptionMenuAppLabel
        appIcon = binding.appOptionMenuAppIcon
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

        show()
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