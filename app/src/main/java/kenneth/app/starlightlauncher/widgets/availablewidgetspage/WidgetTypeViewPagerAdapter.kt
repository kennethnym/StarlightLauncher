package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
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

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> entryPoint.androidWidgetsFragment().apply {
            this.availableWidgetsPageBinding =
                this@WidgetTypeViewPagerAdapter.availableWidgetsPageBinding
        }

        1 -> entryPoint.starlightWidgetsFragment().apply {
            this.availableWidgetsPageBinding =
                this@WidgetTypeViewPagerAdapter.availableWidgetsPageBinding
        }

        else -> throw IndexOutOfBoundsException()
    }
}