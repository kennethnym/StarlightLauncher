package kenneth.app.starlightlauncher.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent

internal const val POSITION_HOME_SCREEN_VIEW_PAGER_HOME = 0
internal const val POSITION_HOME_SCREEN_VIEW_PAGER_ALL_APPS = 1

internal class HomeScreenViewPagerAdapter(
    fa: FragmentActivity,
    private val viewPager: ViewPager2
) : FragmentStateAdapter(fa) {
    @EntryPoint
    @InstallIn(ActivityComponent::class)
    internal interface HomeScreenViewPagerAdapterEntryPoint {
        fun mainScreenFragment(): MainScreenFragment

        fun allAppsScreenFragment(): AllAppsScreenFragment
    }

    private val entryPoint: HomeScreenViewPagerAdapterEntryPoint

    init {
        entryPoint =
            EntryPointAccessors.fromActivity(fa, HomeScreenViewPagerAdapterEntryPoint::class.java)
    }

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
            POSITION_HOME_SCREEN_VIEW_PAGER_HOME -> entryPoint.mainScreenFragment().apply {
                homeScreenViewPager = viewPager
            }
            POSITION_HOME_SCREEN_VIEW_PAGER_ALL_APPS -> entryPoint.allAppsScreenFragment()
            else -> throw IndexOutOfBoundsException()
        }
}