package kenneth.app.spotlightlauncher.prefs.files

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceManager
import kenneth.app.spotlightlauncher.R
import java.lang.Exception

enum class UriAuthority(val str: String) {
    DOWNLOADS("com.android.providers.downloads.documents")
}

object FilePreferenceManager {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var context: Context
    private lateinit var includedPathsPrefKey: String

    lateinit var includedPaths: MutableList<String>
        private set

    private const val PATH_LIST_SEPARATOR = ";"

    fun getInstance(context: Context) = this.apply {
        this.context = context

        if (!::sharedPreferences.isInitialized) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        }

        if (!::includedPathsPrefKey.isInitialized) {
            includedPathsPrefKey = context.getString(R.string.file_search_included_paths)
        }

        if (!::includedPaths.isInitialized) {
            includedPaths = sharedPreferences.getString(includedPathsPrefKey, null)
                ?.split(PATH_LIST_SEPARATOR)?.toMutableList() ?: mutableListOf()
        }
    }

    /**
     * Include a path and save it to SharedPreference.
     *
     * @return true if the path is added, or false if it's not, possibly because the path is
     * already included
     */
    fun addPathWithUri(uri: Uri): Boolean {
        val path = uri.path

        if (path != null) {
            val uriString = uri.toString()
            val isDownloadFolder = uri.authority == UriAuthority.DOWNLOADS.str
            val isDownloadFolderIncluded = includedPaths.any {
                Uri.parse(it).authority == UriAuthority.DOWNLOADS.str
            }

            if (includedPaths.all { it != uriString } || (isDownloadFolder && !isDownloadFolderIncluded)) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                includedPaths.add(uri.toString())
                saveNewIncludedPaths()

                return true
            }
        }

        return false
    }

    /**
     * Remove a path from being included, then save the changes to SharedPreferences.
     *
     * @return true if the path is removed, or false if it's not, possibly because the path is
     * not included in the first place.
     */
    fun removePathWithUri(uri: Uri): Boolean {
        val uriString = uri.toString()

        if (includedPaths.any { it == uriString }) {
            try {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.e("FilePreferenceManager", "Error removing path", e)
                return false
            }

            includedPaths.remove(uriString)
            saveNewIncludedPaths()

            return true
        }

        return false
    }

    private fun saveNewIncludedPaths() {
        sharedPreferences
            .edit()
            .putString(includedPathsPrefKey, includedPaths.joinToString(PATH_LIST_SEPARATOR))
            .apply()
    }
}
