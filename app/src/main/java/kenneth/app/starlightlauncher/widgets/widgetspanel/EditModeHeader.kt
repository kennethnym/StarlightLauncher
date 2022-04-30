package kenneth.app.starlightlauncher.widgets.widgetspanel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.Plate
import kenneth.app.starlightlauncher.databinding.EditModeHeaderBinding
import kenneth.app.starlightlauncher.utils.BindingRegister
import javax.inject.Inject

@AndroidEntryPoint
class EditModeHeader(context: Context, attrs: AttributeSet?) : Plate(context, attrs) {
    @Inject
    lateinit var launcher: StarlightLauncherApi

    private val binding = EditModeHeaderBinding.inflate(LayoutInflater.from(context), this, true)

    init {
        blurWith(launcher.blurHandler)

        setOnApplyWindowInsetsListener { _, insets ->
            binding.editModeHeaderLayout.updatePadding(
                top = WindowInsetsCompat.toWindowInsetsCompat(insets)
                    .getInsets(WindowInsetsCompat.Type.statusBars()).top
            )
            insets
        }

        binding.exitEditModeButton.setOnClickListener {
            BindingRegister.activityMainBinding.widgetsPanel.exitEditMode()
        }
    }
}