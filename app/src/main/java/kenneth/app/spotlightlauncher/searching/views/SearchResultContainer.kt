package kenneth.app.spotlightlauncher.searching.views

import android.content.Context
import android.widget.LinearLayout
import kenneth.app.spotlightlauncher.api.SpotlightLauncherApi
import kenneth.app.spotlightlauncher.api.utils.dp
import kenneth.app.spotlightlauncher.api.view.OptionMenuBuilder
import kenneth.app.spotlightlauncher.api.view.SearchResultAdapter
import kenneth.app.spotlightlauncher.utils.BindingRegister

class SearchResultContainer(context: Context) : LinearLayout(context) {
    /**
     * The [SearchResultAdapter.ViewHolder] this container contains.
     *
     * This is inaccessible until [SearchResultAdapter.onCreateViewHolder] is called.
     */
    var viewHolder: SearchResultAdapter.ViewHolder? = null
        set(vh) {
            field = vh
            addView(vh?.rootView)
        }

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        ).apply {
            setMargins(0, 0, 0, 16.dp)
        }
        orientation = VERTICAL
    }
}