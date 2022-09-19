package kenneth.app.starlightlauncher.api.view

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.*
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import kenneth.app.starlightlauncher.api.R
import kenneth.app.starlightlauncher.api.util.GESTURE_ACTION_THRESHOLD
import kenneth.app.starlightlauncher.api.util.GestureMover
import kenneth.app.starlightlauncher.api.util.activity
import kenneth.app.starlightlauncher.api.util.dp

/**
 * A function that adds content to [OptionMenu], for example adding items to the menu.
 */
typealias OptionMenuBuilder = (menu: OptionMenu) -> Unit

/**
 * A function that is called whenever [OptionMenuItem] is clicked.
 */
typealias OptionMenuItemOnClickListener = (item: OptionMenuItem) -> Unit

/**
 * An option menu on the home screen.
 */
class OptionMenu(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    private var menuHeight = 0

    /**
     * Defines the hide animation used to animate hiding of the menu.
     */
    private lateinit var hideAnimation: SpringAnimation

    /**
     * Defines the animation used to animate the entrance of the menu.
     */
    private val showAnimation =
        SpringAnimation(this, DynamicAnimation.TRANSLATION_Y, 0f)
            .apply {
                spring.apply {
                    dampingRatio = SpringForce.DAMPING_RATIO_NO_BOUNCY
                    stiffness = SpringForce.STIFFNESS_MEDIUM
                }
            }

    private val gestureMover = GestureMover().apply { targetView = this@OptionMenu }

    private val onGlobalLinearLayout = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            viewTreeObserver.removeOnGlobalLayoutListener(this)

            menuHeight = height
            translationY = menuHeight.toFloat()
            alpha = 1f
            hideAnimation = SpringAnimation(
                this@OptionMenu,
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

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            hide()
        }
    }

    init {
        setPadding(24.dp)
        orientation = VERTICAL
        background = ContextCompat.getDrawable(context, R.drawable.option_menu_background)
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL

        // need to measure the height of this view
        // to hide the menu using translation, so we need
        // to hide this view to prevent it from blocking other views.
        // after measurement, we'll set visibility to GONE
        alpha = 0f
        viewTreeObserver.addOnGlobalLayoutListener(onGlobalLinearLayout)

        setOnApplyWindowInsetsListener { _, insets ->
            val navBarHeight =
                WindowInsetsCompat.toWindowInsetsCompat(insets)
                    .getInsets(WindowInsetsCompat.Type.systemBars()).bottom

            updatePadding(
                bottom = 24.dp + navBarHeight
            )

            insets
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean = when (event?.actionMasked) {
        null -> false

        MotionEvent.ACTION_DOWN -> {
            gestureMover.recordInitialEvent(event)
            true
        }

        MotionEvent.ACTION_MOVE -> {
            gestureMover.addMotionMoveEvent(event)
            true
        }

        MotionEvent.ACTION_UP -> {
            gestureMover.addMotionUpEvent(event)

            if (gestureMover.gestureDelta > GESTURE_ACTION_THRESHOLD) {
                hide()
            }

            true
        }

        else -> false
    }

    /**
     * Adds an [OptionMenuItem] to this [OptionMenu].
     * Should be used in the builder of [OptionMenu.show].
     *
     * @param icon The icon of this item. It will be placed at the start of the item.
     * @param label The label of this item. It will be placed next to the item.
     * @param applyIconTint Whether a color tint that matches text color should be applied to [icon].
     *                      Defaults to true.
     * @param onClick The function that is called when this item is clicked.
     */
    fun addItem(
        icon: Drawable?,
        label: String,
        applyIconTint: Boolean = true,
        onClick: OptionMenuItemOnClickListener
    ): OptionMenuItem {
        val item = OptionMenuItem(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT,
            ).apply {
                setMargins(
                    0,
                    0,
                    0,
                    resources.getDimensionPixelSize(R.dimen.option_menu_item_spacing)
                )
            }

            itemIcon = icon
            itemLabel = label
            applyColorTint = applyIconTint

            setOnClickListener { onClick(this) }
        }
        addView(item)

        return item
    }

    /**
     * Shows the option menu with the content added by [builder].
     *
     * To add content to the option menu, you can call [OptionMenu.addView]
     * (or other built-in methods that adds [View] to [OptionMenu]), or use [LayoutInflater]:
     *
     *     // view binding
     *     optionMenu.show { menu ->
     *         MyMenuContentBinding.inflate(LayoutInflater.from(context), menu)
     *     }
     *
     *     // LayoutInflater.inflate
     *     optionMenu.show { menu ->
     *         LayoutInflater.from(context).inflate(R.layout.my_menu, menu)
     *     }
     */
    fun show(builder: OptionMenuBuilder) {
        builder(this)
        isVisible = true
        gestureMover.minY = 0f
        showAnimation.start()
        activity?.onBackPressedDispatcher?.addCallback(onBackPressedCallback)
    }

    /**
     * Hides this menu with an animation. If overriding, must call super to begin the animation.
     * Call super last when cleanup is required before hiding the menu.
     */
    fun hide() {
        onBackPressedCallback.remove()
        with(hideAnimation) {
            addEndListener { _, _, _, _ ->
                this@OptionMenu.apply {
                    isVisible = false
                    removeAllViews()
                }
            }
            start()
        }
    }
}
