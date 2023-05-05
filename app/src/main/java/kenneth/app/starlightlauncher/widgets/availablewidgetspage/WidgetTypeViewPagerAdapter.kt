package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import kenneth.app.starlightlauncher.databinding.FragmentAvailableWidgetsBinding

class WidgetTypeViewPagerAdapter(
    fa: FragmentActivity,
    private val availableWidgetsPageBinding: FragmentAvailableWidgetsBinding,
) :
    FragmentStateAdapter(fa) {
    @EntryPoint
    @InstallIn(ActivityComponent::class)
    internal interface WidgetTypeViewPagerAdapterEntryPoint {
        fun androidWidgetsFragment(): AndroidWidgetsFragment

        fun starlightWidgetsFragment(): StarlightWidgetsFragment
    }

    private val entryPoint =
        EntryPointAccessors.fromActivity(fa, WidgetTypeViewPagerAdapterEntryPoint::class.java)

    private var recyclerView: RecyclerView? = null

    private val globalLayoutListener = object : OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            val bottomPadding = availableWidgetsPageBinding.tabBar.height
            with(entryPoint) {
                androidWidgetsFragment().bottomPadding = bottomPadding
                starlightWidgetsFragment().bottomPadding = bottomPadding
            }
            recyclerView?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
        }
    }

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> entryPoint.androidWidgetsFragment()

        1 -> entryPoint.starlightWidgetsFragment()

        else -> throw IndexOutOfBoundsException()
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
        recyclerView.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)
    }
}