package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import kenneth.app.starlightlauncher.databinding.AvailableWidgetsPageBinding
import kenneth.app.starlightlauncher.databinding.FragmentStarlightWidgetListBinding

internal class StarlightWidgetsFragment(
    private val availableWidgetsPageBinding: AvailableWidgetsPageBinding,
) : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentStarlightWidgetListBinding.inflate(inflater).run {
        widgetList.apply {
            adapter = StarlightWidgetListAdapter(context)
            layoutManager = LinearLayoutManager(context)
        }

        root.setOnApplyWindowInsetsListener { _, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.systemBars())
            widgetList.updatePadding(
                top = insetsCompat.top,
                bottom = availableWidgetsPageBinding.tabBar.height,
            )
            insets
        }

        root
    }
}