package kenneth.app.spotlightlauncher.searching.views

import android.content.Context
import android.content.pm.ResolveInfo
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.SuggestedAppViewBinding

class SuggestedAppView(context: Context) : LinearLayout(context) {
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
        setPadding(resources.getDimensionPixelSize(R.dimen.section_card_padding))

        binding = SuggestedAppViewBinding.inflate(LayoutInflater.from(context), this)

        setOnClickListener { }
    }

    fun setSuggestedApp(appInfo: ResolveInfo) {
        suggestedApp = appInfo
        with(binding) {
            suggestedAppIcon.setImageDrawable(suggestedApp.loadIcon(context.packageManager))
            suggestedAppName.text = suggestedApp.loadLabel(context.packageManager)
        }
    }
}