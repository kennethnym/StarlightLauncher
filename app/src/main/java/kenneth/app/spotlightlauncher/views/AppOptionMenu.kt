package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.setPadding
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
class AppOptionMenu(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var pinnedAppsPreferenceManager: PinnedAppsPreferenceManager

    private lateinit var app: ResolveInfo

    private val appIcon: ImageView
    private val appLabel: TextView
    private var menuHeight = 0

    // menu items
    private val pinAppItem: Item

    private val onGlobalLinearLayout = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)
            menuHeight = height
            translationY = menuHeight.toFloat()
            alpha = 1f
            hideAnimation = SpringAnimation(
                this@AppOptionMenu,
                DynamicAnimation.TRANSLATION_Y,
                translationY
            ).apply {
                spring.apply {
                    dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                    stiffness = SpringForce.STIFFNESS_MEDIUM
                }
            }
        }
    }

    private lateinit var hideAnimation: SpringAnimation
    private val showAnimation = SpringAnimation(this, DynamicAnimation.TRANSLATION_Y, 0f)
        .apply {
            spring.apply {
                dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                stiffness = SpringForce.STIFFNESS_MEDIUM
            }
        }

    init {
        setPadding(24.dp)
        orientation = VERTICAL
        background = context.getDrawable(R.drawable.app_option_menu_background)
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        inflate(context, R.layout.app_option_menu, this)

        appLabel = findViewById(R.id.app_option_menu_app_label)
        appIcon = findViewById(R.id.app_option_menu_app_icon)
        pinAppItem = findViewById(R.id.pin_app_item)

        // need to measure the height of this view
        // to make the slide animation work, so we need
        // to hide this view to prevent it from blocking other views
        // after measurement, we'll set visibility to GONE
        hideSelf()
        viewTreeObserver.addOnGlobalLayoutListener(onGlobalLinearLayout)

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
        showAnimation.start()
    }

    fun hide() {
        with(hideAnimation) {
            addEndListener { _, _, _, _ -> this@AppOptionMenu.isVisible = false }
            start()
        }
    }

    private fun attachListeners() {
        setOnApplyWindowInsetsListener { _, insets ->
            updatePadding(
                bottom = 24.dp + insets.navBarHeight
            )

            insets
        }

        setOnClickListener { hide() }

        pinAppItem.setOnClickListener { togglePin() }
    }

    /**
     * Sets alpha to 0
     */
    private fun hideSelf() {
        alpha = 0f
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

    class Item(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
        private var labelTextView: TextView
        private var iconView: ImageView? = null

        init {
            inflate(context, R.layout.app_option_menu_item, this)

            context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.AppOptionMenuItem,
                0, 0
            ).apply {
                try {
                    val icon = getDrawable(R.styleable.AppOptionMenuItem_icon)
                    val label = getString(R.styleable.AppOptionMenuItem_label)

                    labelTextView = findViewById<TextView>(R.id.item_label).apply {
                        text = label
                    }

                    if (icon != null) {
                        iconView = findViewById<ImageView>(R.id.item_icon).apply {
                            setImageDrawable(icon)
                        }
                    }
                } finally {
                    recycle()
                }
            }
        }

        override fun performClick(): Boolean {
            super.performClick()
            return true
        }

        override fun onTouchEvent(event: MotionEvent?): Boolean {
            super.onTouchEvent(event)

            return when (event?.action) {
                MotionEvent.ACTION_BUTTON_PRESS -> {
                    performClick()
                    true
                }
                MotionEvent.ACTION_DOWN -> {
                    showClickedEffect()
                    true
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL -> {
                    hideClickedEffect()
                    true
                }
                else -> false
            }
        }

        fun setIcon(iconDrawable: Drawable) {
            iconView?.setImageDrawable(iconDrawable)
        }

        fun setLabel(label: String) {
            labelTextView.text = label
        }

        private fun showClickedEffect() {
            alpha = 0.5f
        }

        private fun hideClickedEffect() {
            alpha = 1f
        }
    }
}