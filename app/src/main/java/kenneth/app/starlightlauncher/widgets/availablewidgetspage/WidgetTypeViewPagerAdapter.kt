package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import kenneth.app.starlightlauncher.databinding.AvailableWidgetsPageBinding

class WidgetTypeViewPagerAdapter(
    fa: FragmentActivity,
    private val availableWidgetsPageBinding: AvailableWidgetsPageBinding,
) :
    FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment = when (position) {
        0 -> AndroidWidgetsFragment(availableWidgetsPageBinding)
        1 -> StarlightWidgetsFragment(availableWidgetsPageBinding)
        else -> throw IndexOutOfBoundsException()
    }
}