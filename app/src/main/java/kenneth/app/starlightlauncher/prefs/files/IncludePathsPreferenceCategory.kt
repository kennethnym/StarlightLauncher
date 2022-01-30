package kenneth.app.starlightlauncher.prefs.files

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder

/**
 * Extends PreferenceCategory to allow multiline summary.
 */
class IncludePathsPreferenceCategory(context: Context, attrs: AttributeSet?) :
    PreferenceCategory(context, attrs) {
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        (holder?.findViewById(android.R.id.summary) as? TextView)?.apply {
            isSingleLine = false
            maxLines = 10
        }
    }
}