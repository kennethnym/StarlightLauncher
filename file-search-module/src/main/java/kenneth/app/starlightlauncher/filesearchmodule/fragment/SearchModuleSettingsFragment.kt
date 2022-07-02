package kenneth.app.starlightlauncher.filesearchmodule.fragment

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import kenneth.app.starlightlauncher.filesearchmodule.FileSearchModulePreferences
import kenneth.app.starlightlauncher.filesearchmodule.R
import kenneth.app.starlightlauncher.filesearchmodule.UriAuthority

class SearchModuleSettingsFragment : PreferenceFragmentCompat() {
    private lateinit var filePickerLauncher: ActivityResultLauncher<Uri>
    private lateinit var pathPrefCategory: PreferenceCategory

    private var fileSearchModulePreferences: FileSearchModulePreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.search_module_root_preferences, rootKey)

        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
            ::handlePickedFolder
        )

        context?.let {
            fileSearchModulePreferences = FileSearchModulePreferences.getInstance(it)
        }

        findPreference<Preference>(getString(R.string.pref_key_add_path))?.apply {
            isEnabled = fileSearchModulePreferences != null
            setOnPreferenceClickListener {
                openFilePicker()
                true
            }
        }

        findPreference<PreferenceCategory>(getString(R.string.pref_key_category_included_paths))
            ?.let {
                pathPrefCategory = it
            }

        loadIncludedPaths()
    }

    private fun loadIncludedPaths() {
        fileSearchModulePreferences?.includedPaths?.forEach {
            addPathPreference(Uri.parse(it))
        }
    }

    private fun openFilePicker() {
        filePickerLauncher.launch(Uri.EMPTY)
    }

    private fun handlePickedFolder(uri: Uri?) {
        if (uri != null) {
            val added = fileSearchModulePreferences?.includeNewPath(uri) == true
            if (added) {
                addPathPreference(uri)
            }
        }
    }

    private fun addPathPreference(uri: Uri) {
        val context = this.context ?: return
        pathPrefCategory.addPreference(
            Preference(context)
                .apply {
                    key = uri.toString()
                    title = when (uri.authority) {
                        UriAuthority.DOWNLOADS.str -> getString(R.string.download_folder_name)
                        else -> {
                            val path = uri.path!!.split(":").last()
                            if (path == "") "Main storage" else path
                        }
                    }
                }
                .also { pref -> pref.setOnPreferenceClickListener { removePath(it) } }
        )
    }


    private fun removePath(preference: Preference): Boolean {
        val uri = Uri.parse(preference.key)
        val removed = fileSearchModulePreferences?.removePath(uri) == true

        if (removed) {
            pathPrefCategory.removePreference(preference)
        }

        return true
    }
}