package kenneth.app.starlightlauncher.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import kenneth.app.starlightlauncher.AllAppsScreenFragment

internal class HomeScreenViewPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    @EntryPoint
    @InstallIn(ActivityComponent::class)
    internal interface HomeScreenViewPagerAdapterEntryPoint {
        fun mainScreenFragment(): MainScreenFragment
    }

    private val entryPoint: HomeScreenViewPagerAdapterEntryPoint

    init {
        entryPoint =
            EntryPointAccessors.fromActivity(fa, HomeScreenViewPagerAdapterEntryPoint::class.java)
    }

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> entryPoint.mainScreenFragment()
            1 -> AllAppsScreenFragment()
            else -> throw IndexOutOfBoundsException()
        }
}