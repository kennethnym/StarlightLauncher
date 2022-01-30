package kenneth.app.starlightlauncher.prefs.searchlayout

import android.content.Context
import android.util.AttributeSet
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import kenneth.app.starlightlauncher.spotlightlauncher.R

class SearchCategoryOrderPreference(context: Context, attrs: AttributeSet?) :
    PreferenceCategory(context, attrs) {
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        (holder?.findViewById(R.id.search_category_order_list) as? SearchCategoryOrderList)
            ?.showItems()
    }
}
