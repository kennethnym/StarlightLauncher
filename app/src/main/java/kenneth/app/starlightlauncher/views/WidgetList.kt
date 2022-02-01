package kenneth.app.starlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.spotlightlauncher.R
import kenneth.app.starlightlauncher.animations.CardAnimation
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.extension.ExtensionManager
import javax.inject.Inject

/**
 * Contains a list of widgets on the home screen.
 */
@AndroidEntryPoint
class WidgetList(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs) {
    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var launcherApi: StarlightLauncherApi

    private val animations: List<CardAnimation>
    private val loadedWidgets = mutableListOf<WidgetHolder>()

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL

        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.widget_margin_horizontal)

        setPadding(paddingHorizontal, 0, paddingHorizontal, 0)
        inflate(context, R.layout.widget_list, this)
        loadWidgets()

        animations = generateAnimations()
    }

    /**
     * Shows all the widgets in this list.
     */
    fun showWidgets() {
        animations.forEach { it.showCard() }
    }

    /**
     * Hides all the widgets in this list. Note that this does not remove children in the layout.
     */
    fun hideWidgets() {
        animations.forEach { it.hideCard() }
    }

    private fun loadWidgets() {
        extensionManager.installedWidgets.forEach { creator ->
            val widget = creator.createWidget(this, launcherApi)
            loadedWidgets += widget
            widget.rootView.run {
                layoutParams = ViewGroup.MarginLayoutParams(layoutParams).apply {
                    bottomMargin =
                        context.resources.getDimensionPixelSize(R.dimen.widget_list_spacing)
                }
                addView(this)
            }
        }
    }

    /**
     * Generates card animations for every widget.
     */
    private fun generateAnimations(): List<CardAnimation> =
        children.foldIndexed(mutableListOf()) { i, anims, child ->
            if (child.isVisible) {
                anims.add(CardAnimation(child, i * 20L))
            }
            anims
        }
}
