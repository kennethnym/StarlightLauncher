package kenneth.app.starlightlauncher.prefs.files

import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.spotlightlauncher.R
import javax.inject.Inject

@AndroidEntryPoint
class FileSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var filePreferenceManager: FilePreferenceManager

    private lateinit var filePickerLauncher: ActivityResultLauncher<Uri>

    private var pathPrefCategory: PreferenceCategory? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.OpenDocumentTree(),
            ::handlePickedFolder
        )

        pathPrefCategory = findPreference(getString(R.string.file_include_paths_category))

        filePreferenceManager.includedSearchPaths.forEach { uriString ->
            addPathPreference(
                Uri.parse(
                    uriString
                )
            )
        }

        findPreference<Preference>(getString(R.string.file_search_add_path))
            ?.setOnPreferenceClickListener { openFilePicker() }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.file_search_preferences, rootKey)
        changeToolbarTitle()
    }

    override fun onDestroy() {
        activity
            ?.findViewById<MaterialToolbar>(R.id.settings_toolbar)
            ?.title = getString(R.string.title_activity_settings)

        super.onDestroy()
    }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.file_search_title)
    }

    private fun openFilePicker(): Boolean {
        filePickerLauncher.launch(Uri.EMPTY)
        return true
    }

    private fun removePath(preference: Preference): Boolean {
        val uri = Uri.parse(preference.key)
        val removed = filePreferenceManager.removePathWithUri(uri)

        if (removed) {
            pathPrefCategory?.removePreference(preference)
        }

        return true
    }

    private fun handlePickedFolder(uri: Uri?) {
        if (uri != null) {
            val added = filePreferenceManager.addPathWithUri(uri)

            if (added) {
                addPathPreference(uri)
            }
        }
    }

    private fun addPathPreference(uri: Uri) {
        pathPrefCategory?.addPreference(
            Preference(context)
                .apply {
                    key = uri.toString()
                    title = when (uri.authority) {
                        UriAuthority.DOWNLOADS.str -> getString(R.string.download_folder)
                        else -> {
                            val path = uri.path!!.split(":").last()
                            if (path == "") "Main storage" else path
                        }
                    }
                }
                .also { pref -> pref.setOnPreferenceClickListener { removePath(it) } }
        )
    }
}
