package kenneth.app.starlightlauncher.filesearchmodule

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import java.util.*

internal class FileSearchModulePreferences
private constructor(private val context: Context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: FileSearchModulePreferences? = null

        internal fun getInstance(context: Context) =
            instance ?: run {
                FileSearchModulePreferences(context.applicationContext)
                    .also { instance = it }
            }
    }

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val keys = PrefKeys(context)

    private val _includedPaths =
        sharedPreferences.getStringSet(keys.includedPaths, mutableSetOf()) ?: mutableSetOf()

    val includedPaths
        get() = _includedPaths.toList()

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
            } finally {
                _includedPaths.remove(uriString)
                saveNewIncludedPaths()
            }
            return true
        }

        return false
    }

    private fun saveNewIncludedPaths() {
        sharedPreferences.edit(commit = true) {
            putStringSet(keys.includedPaths, _includedPaths)
        }
    }
}

internal class PrefKeys(context: Context) {
    val includedPaths = context.getString(R.string.pref_key_included_paths)
}