package kenneth.app.starlightlauncher.prefs.files

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.spotlightlauncher.R
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

enum class UriAuthority(val str: String) {
    DOWNLOADS("com.android.providers.downloads.documents")
}

@Singleton
class FilePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    private val includedPathsPrefKey =
        context.getString(R.string.file_search_included_paths)

    private val pathListSeparator = ";"

    private val includedPaths = sharedPreferences.getString(includedPathsPrefKey, null)
        ?.split(pathListSeparator)
        ?.toMutableList()
        ?: mutableListOf()

    /**
     * Paths included for file searching.
     * Any path not included in this list will be ignored.
     */
    val includedSearchPaths
        get() = includedPaths.toList()

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

        if (includedSearchPaths.any { it == uriString }) {
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
            .putString(includedPathsPrefKey, includedSearchPaths.joinToString(pathListSeparator))
            .apply()
    }
}
