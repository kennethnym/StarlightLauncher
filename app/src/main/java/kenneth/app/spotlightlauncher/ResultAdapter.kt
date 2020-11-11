package kenneth.app.spotlightlauncher

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/**
 * ResultAdapter is used to adapt search results into views. It combines numerous RecyclerView
 * adapters into one class.
 */
class ResultAdapter(activity: MainActivity) {
    private val appsGridAdapter = AppsGridAdapter.initializeWith(activity)
    private val fileListAdapter = FileListAdapter.initializeWith(activity)

    fun displayResult(result: Searcher.Result, type: SearchType) {
        when (type) {
            SearchType.ALL -> {
                appsGridAdapter.displayResult(result.apps)
                fileListAdapter.displayResult(result.files)
            }
            SearchType.FILES -> {
                fileListAdapter.displayResult(result.files)
            }
            SearchType.APPS -> {
                appsGridAdapter.displayResult(result.apps)
            }
        }
    }
}