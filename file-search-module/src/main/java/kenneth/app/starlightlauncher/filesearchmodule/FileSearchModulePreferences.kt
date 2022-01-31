package kenneth.app.starlightlauncher.filesearchmodule

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager

internal class FileSearchModulePreferences(private val context: Context) {
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val keys = PrefKeys(context)

    private val _includedPaths =
        prefs.getStringSet(keys.includedPaths, mutableSetOf()) ?: mutableSetOf()

    val includedPaths = _includedPaths.toList()

    /**
     * Include a path and save it to SharedPreference.
     *
     * @return true if the path is added, or false if it's not, possibly because the path is
     * already included
     */
    fun includeNewPath(uri: Uri): Boolean {
        val path = uri.path

        if (path != null) {
            val uriString = uri.toString()
            val isDownloadFolder = uri.authority == UriAuthority.DOWNLOADS.str
            val isDownloadFolderIncluded = includedPaths.any {
                Uri.parse(it).authority == UriAuthority.DOWNLOADS.str
            }

            if (!includedPaths.contains(uriString) || (isDownloadFolder && !isDownloadFolderIncluded)) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                _includedPaths.add(uri.toString())
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
    fun removePath(uri: Uri): Boolean {
        val uriString = uri.toString()

        if (_includedPaths.contains(uriString)) {
            try {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                Log.e("FilePreferenceManager", "Error removing path", e)
                return false
            }

            _includedPaths.remove(uriString)
            saveNewIncludedPaths()

            return true
        }

        return false
    }

    private fun saveNewIncludedPaths() {
        prefs.edit(commit = true) {
            putStringSet(keys.includedPaths, _includedPaths)
        }
    }
}

internal class PrefKeys(context: Context) {
    val includedPaths = context.getString(R.string.pref_key_included_paths)
}