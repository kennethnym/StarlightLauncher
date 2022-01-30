package kenneth.app.starlightlauncher.searching.views

import android.content.Context
import android.content.pm.ResolveInfo
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.spotlightlauncher.R
import kenneth.app.starlightlauncher.spotlightlauncher.databinding.SuggestedAppViewBinding
import kenneth.app.starlightlauncher.prefs.appearance.AppearancePreferenceManager
import javax.inject.Inject

@AndroidEntryPoint
class SuggestedAppView(context: Context) : LinearLayout(context) {
    @Inject
    lateinit var appearancePreferenceManager: AppearancePreferenceManager

    private lateinit var suggestedApp: ResolveInfo

    private val binding: SuggestedAppViewBinding

    init {
        val bgDrawableId = TypedValue().run {
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, this, true)
            resourceId
        }
        background = ContextCompat.getDrawable(context, bgDrawableId)
        isClickable = true
        isFocusable = true
        gravity = Gravity.CENTER_VERTICAL
        orientation = HORIZONTAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        setPadding(resources.getDimensionPixelSize(R.dimen.card_padding))

        binding = SuggestedAppViewBinding.inflate(LayoutInflater.from(context), this)

        setOnClickListener { }
    }

    fun setSuggestedApp(appInfo: ResolveInfo) {
        suggestedApp = appInfo
        with(binding) {
            suggestedAppIcon.setImageBitmap(
                appearancePreferenceManager.iconPack.getIconOf(appInfo)
            )
            suggestedAppName.text = suggestedApp.loadLabel(context.packageManager)
        }
    }
}
