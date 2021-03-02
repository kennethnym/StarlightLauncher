package kenneth.app.spotlightlauncher.views

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.CallSuper
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kenneth.app.spotlightlauncher.GESTURE_ACTION_THRESHOLD
import kenneth.app.spotlightlauncher.HANDLED
import kenneth.app.spotlightlauncher.NOT_HANDLED
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.GestureMover
import kenneth.app.spotlightlauncher.utils.dp
import kenneth.app.spotlightlauncher.utils.navBarHeight

/**
 * An option menu at the bottom with a shadow gradient background.
 */
open class BottomOptionMenu(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    /**
     * Determines if menu is currently being (partially or fully) shown on the screen.
     */
    val isActive: Boolean
        get() = translationY != menuHeight.toFloat()

    private val gestureMover = GestureMover().apply {
        targetView = this@BottomOptionMenu
        minY = 0f
    }

    private var menuHeight = 0

    private val onGlobalLinearLayout = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)

            menuHeight = height
            translationY = menuHeight.toFloat()
            alpha = 1f
            hideAnimation = SpringAnimation(
                this@BottomOptionMenu,
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

    /**
     * Defines the hide animation used to animate hiding of the menu.
     */
    private lateinit var hideAnimation: SpringAnimation

    /**
     * Defines the animation used to animate entrance of the menu.
     */
    private val showAnimation by lazy {
        SpringAnimation(this, DynamicAnimation.TRANSLATION_Y, 0f)
            .apply {
                spring.apply {
                    dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                    stiffness = SpringForce.STIFFNESS_MEDIUM
                }
            }
    }

    init {
        setPadding(24.dp)
        orientation = VERTICAL
        background = context.getDrawable(R.drawable.bottom_option_menu_background)
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        // need to measure the height of this view
        // to hide the menu using translation, so we need
        // to hide this view to prevent it from blocking other views.
        // after measurement, we'll set visibility to GONE
        alpha = 0f
        viewTreeObserver.addOnGlobalLayoutListener(onGlobalLinearLayout)

        setOnApplyWindowInsetsListener { _, insets ->
            updatePadding(
                bottom = 24.dp + insets.navBarHeight
            )

            insets
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.actionMasked) {
            null -> NOT_HANDLED

            MotionEvent.ACTION_DOWN -> {
                gestureMover.recordInitialEvent(event)
                HANDLED
            }

            MotionEvent.ACTION_MOVE -> {
                gestureMover.addMotionMoveEvent(event)
                HANDLED
            }

            MotionEvent.ACTION_UP -> {
                gestureMover.addMotionUpEvent(event)

                val gestureDistance = gestureMover.gestureDelta

                Log.d("hub", "gesture distance $gestureDistance")

                if (gestureDistance > GESTURE_ACTION_THRESHOLD) {
                    hide()
                }

                HANDLED
            }

            else -> NOT_HANDLED
        }
    }

    override fun onKeyPreIme(keyCode: Int, event: KeyEvent?) =
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                hide()
                HANDLED
            }
            else -> NOT_HANDLED
        }

    /**
     * Shows this menu with an animation. If overriding, must call super to begin the animation.
     * Call super last when initialization logic is required before showing the menu.
     */
    fun show() {
        isVisible = true
        showAnimation.start()
    }

    /**
     * Hides this menu with an animation. If overriding, must call super to begin the animation.
     * Call super last when cleanup is required before hiding the menu.
     */
    fun hide() {
        with(hideAnimation) {
            addEndListener { _, _, _, _ -> this@BottomOptionMenu.isVisible = false }
            start()
        }
    }

    class Item(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
        private var labelTextView: TextView
        private var iconView: ImageView? = null

        init {
            inflate(context, R.layout.bottom_option_menu_item, this)

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

        /**
         * Sets the icon of this menu item.
         */
        fun setIcon(iconDrawable: Drawable) {
            iconView?.setImageDrawable(iconDrawable)
        }

        /**
         * Sets the label of this menu item.
         */
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