package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import kenneth.app.starlightlauncher.databinding.FragmentAvailableWidgetsBinding

internal class AvailableWidgetsFragment : Fragment() {
    private var binding: FragmentAvailableWidgetsBinding? = null

    private val viewPagerPageListener = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            binding?.selectedPage = position
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = activity?.let { activity ->
        FragmentAvailableWidgetsBinding.inflate(inflater, container, false).run {
            binding = this
            selectedPage = viewPager.currentItem

            androidWidgetsTab.setOnClickListener { viewPager.currentItem = 0 }
            starlightWidgetsTab.setOnClickListener { viewPager.currentItem = 1 }

            val insets =
                WindowInsetsCompat.toWindowInsetsCompat(activity.window.decorView.rootWindowInsets)
                    .getInsets(WindowInsetsCompat.Type.systemBars())

            tabBar.updatePadding(top = insets.bottom * 2, bottom = insets.bottom * 2)

            viewPager.adapter = WidgetTypeViewPagerAdapter(activity, this)
            viewPager.registerOnPageChangeCallback(viewPagerPageListener)

            root
        }
    }
}