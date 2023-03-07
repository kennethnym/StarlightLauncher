package kenneth.app.starlightlauncher.filesearchmodule

import android.annotation.SuppressLint
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kenneth.app.starlightlauncher.filesearchmodule.settings.PREF_KEY_INCLUDED_SEARCH_PATHS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.*

internal class FileSearchModulePreferences
private constructor(private val dataStore: DataStore<Preferences>) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: FileSearchModulePreferences? = null

        internal fun getInstance(dataStore: DataStore<Preferences>) =
            instance ?: run {
                FileSearchModulePreferences(dataStore)
                    .also { instance = it }
            }
    }

    val includedPaths = dataStore.data.map {
        it[PREF_KEY_INCLUDED_SEARCH_PATHS] ?: setOf()
    }

    /**
     * Include a path and save it to SharedPreference.
     */
    suspend fun includeNewPath(uri: Uri) {
        val path = uri.path
        val currentIncludedPaths = includedPaths.first()

        if (path != null) {
            val uriString = uri.toString()
            val isDownloadFolder = uri.authority == DocumentProviderName.DOWNLOADS.authority
            val isDownloadFolderIncluded = currentIncludedPaths.any {
                Uri.parse(it).authority == DocumentProviderName.DOWNLOADS.authority
            }

            if (!currentIncludedPaths.contains(uriString) || (isDownloadFolder && !isDownloadFolderIncluded)) {
                dataStore.edit {
                    it[PREF_KEY_INCLUDED_SEARCH_PATHS] = currentIncludedPaths + uri.toString()
                }
            }
        }
    }

    /**
     * Remove a path from being included, then save the changes to SharedPreferences.
     */
    suspend fun removePath(uri: Uri) {
        val uriString = uri.toString()
        dataStore.edit {
            it[PREF_KEY_INCLUDED_SEARCH_PATHS]?.let { searchPaths ->
                it[PREF_KEY_INCLUDED_SEARCH_PATHS] = searchPaths - uriString
            }
        }
    }
}
