package kenneth.app.starlightlauncher.views

import android.animation.ObjectAnimator
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.animations.CardAnimation
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.widgets.AddedWidget
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceChanged
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
import javax.inject.Inject

/**
 * Contains a list of widgets on the home screen.
 */
@AndroidEntryPoint
class WidgetList(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {
    @Inject
    lateinit var widgetPreferenceManager: WidgetPreferenceManager

    private val animations: List<CardAnimation>

    private val appWidgetManager = AppWidgetManager.getInstance(context)

    private val widgetListAdapter: WidgetListAdapter

    private val showAnimator = ObjectAnimator.ofFloat(this, "alpha", 0f, 1f).apply {
        duration = 200
    }

    private val hideAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f).apply {
        duration = 200
    }

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        )
        layoutManager = LinearLayoutManager(context)
        adapter = WidgetListAdapter(context).also { widgetListAdapter = it }
        animations = generateAnimations()

        val paddingHorizontal = resources.getDimensionPixelSize(R.dimen.widget_margin_horizontal)
        setPadding(paddingHorizontal, paddingHorizontal, paddingHorizontal, 0)
//        loadWidgets()

        widgetPreferenceManager.addOnWidgetPreferenceChangedListener {
            when (it) {
                is WidgetPreferenceChanged.WidgetOrderChanged -> {
//                    onWidgetOrderChanged(it.fromPosition, it.toPosition)
                }
                is WidgetPreferenceChanged.NewAndroidWidgetAdded -> {
                    onAndroidWidgetAdded(it.addedWidget)
                }
            }
        }
    }

    /**
     * Shows all the widgets in this list.
     */
    fun showWidgets() {
//        animations.forEach { it.showCard() }
        showAnimator.start()
    }

    /**
     * Hides all the widgets in this list. Note that this does not remove children in the layout.
     */
    fun hideWidgets() {
//        animations.forEach { it.hideCard() }
        hideAnimator.start()
    }

    private fun loadWidgets() {
        val appWidgetProviders = appWidgetManager.installedProviders
            .fold(mutableMapOf<String, AppWidgetProviderInfo>()) { m, info ->
                m.apply {
                    put(info.provider.flattenToString(), info)
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

//    private fun onWidgetOrderChanged(fromPosition: Int, toPosition: Int) {
//        swapContainers(fromPosition, toPosition)
//    }

    private fun onAndroidWidgetAdded(widget: AddedWidget.AndroidWidget) {
        widgetListAdapter.addAndroidWidget(widget)
    }

    /**
     * Generates card animations for every widget.
     */
    private fun generateAnimations(): List<CardAnimation> =
        children.foldIndexed(mutableListOf()) { i, anims, child ->
            anims.apply { add(CardAnimation(child, i * 20L)) }
        }
}
