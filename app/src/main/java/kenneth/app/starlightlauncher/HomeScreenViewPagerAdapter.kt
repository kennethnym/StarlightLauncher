package kenneth.app.starlightlauncher

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import dagger.hilt.android.scopes.ActivityScoped

internal class HomeScreenViewPagerAdapter(
    fa: FragmentActivity,
    private val bindingRegister: BindingRegister
) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment =
        when (position) {
            0 -> MainScreenFragment(bindingRegister)
            1 -> AllAppsScreenFragment()
            else -> throw IndexOutOfBoundsException()
        }
}