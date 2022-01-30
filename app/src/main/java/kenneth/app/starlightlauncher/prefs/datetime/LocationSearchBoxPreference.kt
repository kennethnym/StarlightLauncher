package kenneth.app.starlightlauncher.prefs.datetime

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import androidx.core.view.isInvisible
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import kenneth.app.starlightlauncher.spotlightlauncher.R

typealias OnRequestSearchListener = (query: String) -> Unit

class LocationSearchBoxPreference(context: Context, attrs: AttributeSet) :
    Preference(context, attrs) {
    /**
     * Determines whether the progress bar is visible.
     */
    var isLoading: Boolean = false
        set(loading) {
            field = loading
            searchProgressBar?.let {
                it.isInvisible = !loading
            }
        }

    private lateinit var onRequestSearchListener: OnRequestSearchListener
    private var searchProgressBar: ProgressBar? = null

    init {
        layoutResource = R.layout.location_search_box
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder?) {
        super.onBindViewHolder(holder)
        holder?.let {
            it.itemView.isClickable = false
            (it.findViewById(R.id.location_search_box) as EditText)
                .setOnEditorActionListener { textView, action, _ ->
                    if (action == EditorInfo.IME_ACTION_SEARCH && ::onRequestSearchListener.isInitialized) {
                        onRequestSearchListener(textView.text.toString())
                        true
                    } else false
                }

            searchProgressBar =
                it.findViewById(R.id.location_search_progress_indicator) as ProgressBar
        }
    }

    /**
     * Registers a listener that is fired when the search button is pressed on the soft keyboard.
     */
    fun setSearchRequestListener(listener: OnRequestSearchListener) {
        onRequestSearchListener = listener
    }
}