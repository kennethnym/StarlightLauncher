package kenneth.app.starlightlauncher.setup

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

internal class SetupPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = SETUP_STEP_COUNT


    override fun createFragment(position: Int): Fragment = SETUP_PAGE_CONSTRUCTORS[position]()
}