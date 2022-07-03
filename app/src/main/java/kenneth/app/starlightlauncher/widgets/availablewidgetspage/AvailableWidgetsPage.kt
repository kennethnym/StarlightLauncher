package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.content.Context
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import kenneth.app.starlightlauncher.databinding.AvailableWidgetsPageBinding
import kenneth.app.starlightlauncher.util.activity

internal class AvailableWidgetsPage(context: Context) : ConstraintLayout(context) {
    private val binding =
        AvailableWidgetsPageBinding.inflate(LayoutInflater.from(context), this, true)

    private val viewPagerPageListener = object : OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            binding.selectedPage = position
        }
    }

    init {
        activity?.let {
            with(binding) {
                viewPager.adapter = WidgetTypeViewPagerAdapter(it, binding)
                viewPager.registerOnPageChangeCallback(viewPagerPageListener)

                selectedPage = viewPager.currentItem

                androidWidgetsTab.setOnClickListener { viewPager.currentItem = 0 }
                starlightWidgetsTab.setOnClickListener { viewPager.currentItem = 1 }
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val insets = WindowInsetsCompat.toWindowInsetsCompat(rootWindowInsets)
            .getInsets(WindowInsetsCompat.Type.systemBars())
        binding.tabBar.updatePadding(top = insets.bottom * 2, bottom = insets.bottom * 2)
    }
}