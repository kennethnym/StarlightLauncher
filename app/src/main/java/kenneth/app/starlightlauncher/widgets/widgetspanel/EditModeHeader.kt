package kenneth.app.starlightlauncher.widgets.widgetspanel

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.Plate
import kenneth.app.starlightlauncher.databinding.EditModeHeaderBinding
import javax.inject.Inject

/**
 * Header that is shown when widget edit mode is active.
 */
@AndroidEntryPoint
internal class EditModeHeader(context: Context, attrs: AttributeSet?) : Plate(context, attrs) {
    @Inject
    lateinit var launcher: StarlightLauncherApi

    var onRequestExitEditMode: (() -> Unit)? = null

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
            onRequestExitEditMode?.let { it() }
        }
    }
}