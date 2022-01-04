package kenneth.app.spotlightlauncher.prefs.searchlayout

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceViewHolder
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.SearchCategoryOrderListItemBinding
import kenneth.app.spotlightlauncher.prefs.views.PreferenceCategoryWithSummary
import kenneth.app.spotlightlauncher.searching.Searcher
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter

class SearchCategoryOrderPreference(context: Context, attrs: AttributeSet?) :
    PreferenceCategory(context, attrs) {
    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)

        (holder?.findViewById(R.id.search_category_order_list) as? SearchCategoryOrderList)
            ?.showItems()
    }
}
