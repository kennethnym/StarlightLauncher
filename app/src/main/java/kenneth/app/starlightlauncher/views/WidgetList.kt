package kenneth.app.starlightlauncher.views

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.children
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.animations.CardAnimation
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetHolder
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.prefs.widget.WidgetPreferenceChanged
import kenneth.app.starlightlauncher.prefs.widget.WidgetPreferenceManager
import javax.inject.Inject

/**
 * Contains a list of widgets on the home screen.
 */
@AndroidEntryPoint
class WidgetList(context: Context, attrs: AttributeSet) :
    OrderedInsertionLinearLayout(context, attrs) {
    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var launcherApi: StarlightLauncherApi

    @Inject
    lateinit var widgetPreferenceManager: WidgetPreferenceManager

    override val allContainers: MutableList<Container?> =
        extensionManager.installedExtensions
            .filter { it.widget != null }
            .map { null }
            .toMutableList()

    private val animations: List<CardAnimation>
    private val loadedWidgets = mutableListOf<WidgetHolder>()

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        )

        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.widget_margin_horizontal)

        setPadding(paddingHorizontal, paddingHorizontal, paddingHorizontal, 0)
        loadWidgets()

        animations = generateAnimations()

        widgetPreferenceManager.addOnWidgetPreferenceChangedListener {
            when (it) {
                is WidgetPreferenceChanged.WidgetOrderChanged -> {
                    onWidgetOrderChanged(it.fromPosition, it.toPosition)
                }
            }
        }
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
        widgetPreferenceManager.widgetOrder.forEachIndexed { i, extName ->
            extensionManager.lookupWidget(extName)?.let {
                Log.d("starlight", "createContainerAt $i")
                val container = createContainerAt(i).apply {
                    layoutParams = LayoutParams(layoutParams).apply {
                        bottomMargin =
                            context.resources.getDimensionPixelSize(R.dimen.widget_list_spacing)
                    }
                }
                val widget = it.createWidget(container, launcherApi)
                container.addView(widget.rootView)
                loadedWidgets += widget
            }
        }
//        extensionManager.installedWidgets.forEach { creator ->
//            val widget = creator.createWidget(this, launcherApi)
//            loadedWidgets += widget
//            widget.rootView.run {
//                layoutParams = MarginLayoutParams(layoutParams).apply {
//                    bottomMargin =
//                        context.resources.getDimensionPixelSize(R.dimen.widget_list_spacing)
//                }
//                addView(this)
//            }
//        }
    }

    private fun onWidgetOrderChanged(fromPosition: Int, toPosition: Int) {
        swapContainers(fromPosition, toPosition)
    }

    /**
     * Generates card animations for every widget.
     */
    private fun generateAnimations(): List<CardAnimation> =
        children.foldIndexed(mutableListOf()) { i, anims, child ->
            anims.apply { add(CardAnimation(child, i * 20L)) }
        }
}
