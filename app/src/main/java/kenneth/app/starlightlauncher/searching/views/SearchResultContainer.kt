package kenneth.app.starlightlauncher.searching.views

import android.content.Context
import android.widget.LinearLayout
import kenneth.app.starlightlauncher.api.utils.dp
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter

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

    /**
     * The index at which this container should appear in the search result list
     */
    var order: Int = 0

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