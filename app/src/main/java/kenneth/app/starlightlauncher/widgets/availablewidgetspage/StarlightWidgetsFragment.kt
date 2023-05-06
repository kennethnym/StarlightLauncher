package kenneth.app.starlightlauncher.widgets.availablewidgetspage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import kenneth.app.starlightlauncher.databinding.FragmentAvailableWidgetsBinding
import kenneth.app.starlightlauncher.databinding.FragmentStarlightWidgetListBinding
import kenneth.app.starlightlauncher.widgets.WidgetPreferenceManager
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class StarlightWidgetsFragment @Inject constructor(
    private val widgetPreferenceManager: WidgetPreferenceManager
) : Fragment(),
    StarlightWidgetListAdapter.Callback {
    private var binding: FragmentStarlightWidgetListBinding? = null

    var availableWidgetsPageBinding: FragmentAvailableWidgetsBinding? = null

    private val globalLayoutListener = object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            binding?.widgetList?.updatePadding(
                bottom = availableWidgetsPageBinding?.tabBar?.height ?: 0,
            )
            binding?.root?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentStarlightWidgetListBinding.inflate(inflater).run {
        widgetList.apply {
            adapter = StarlightWidgetListAdapter(context, this@StarlightWidgetsFragment)
            layoutManager = LinearLayoutManager(context)
        }

        root.setOnApplyWindowInsetsListener { _, insets ->
            val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
                .getInsets(WindowInsetsCompat.Type.systemBars())
            widgetList.updatePadding(top = insetsCompat.top)
            insets
        }

        root.viewTreeObserver.addOnGlobalLayoutListener(globalLayoutListener)

        root
    }

    override fun onRequestAddWidget(extensionName: String) {
        lifecycleScope.launch {
            widgetPreferenceManager.addStarlightWidget(extensionName)
        }
    }

    override fun onRequestRemoveWidget(extensionName: String) {
        lifecycleScope.launch {
            widgetPreferenceManager.removeStarlightWidget(extensionName)
        }
    }
}