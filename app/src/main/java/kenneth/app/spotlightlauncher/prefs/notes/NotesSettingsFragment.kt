package kenneth.app.spotlightlauncher.prefs.notes

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.HANDLED
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.dp
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

private const val NOTES_JSON_FILE_PREFIX = "HubLauncher_Notes_"

@AndroidEntryPoint
class NotesSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var notesPreferenceManager: NotesPreferenceManager

    private val notesJSONFileTimestampFormat = SimpleDateFormat("Md_y_kms", Locale.getDefault())

    private val fileSaveIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result?.data?.data?.let { writeJSONFile(it) }
            }
        }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.quick_notes_preferences, rootKey)
        changeToolbarTitle()

        findPreference<Preference>(getString(R.string.quick_notes_export))
            ?.setOnPreferenceClickListener {
                exportNotes()
                HANDLED
            }
    }

    private fun exportNotes() {
        if (notesPreferenceManager.notesJSON != null) {
            showFileNameDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        changeToolbarTitle()
    }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.quick_notes_title)
    }

    private fun showFileNameDialog() {
        context?.let { context ->
            val defaultFileName =
                NOTES_JSON_FILE_PREFIX + notesJSONFileTimestampFormat.format(Date())
            val fileNameEditText = TextInputEditText(context)
            val fileNameEditTextLayout = TextInputLayout(
                context,
                null,
                R.style.Widget_MaterialComponents_TextInputLayout_FilledBox
            ).apply {
                hint = "test"
                addView(fileNameEditText)
                setPadding(24.dp, 0, 24.dp, 0)
            }

            AlertDialog.Builder(context).run {
                setTitle(getString(R.string.quick_notes_file_name_dialog_title))
                setMessage(
                    getString(
                        R.string.quick_notes_file_name_dialog_message,
                        defaultFileName
                    )
                )
                setView(fileNameEditTextLayout)

                setPositiveButton(getString(R.string.quick_notes_file_name_dialog_positive_label)) { _, _ ->
                    val inputtedName = fileNameEditText.text.toString()
                    if (inputtedName.isNotBlank()) {
                        fileNameEditText.error = null
                        createJSONFile(fileName = inputtedName)
                    } else {
                        fileNameEditText.error =
                            getString(R.string.quick_notes_file_name_dialog_empty_name_error)
                    }
                }

                setNegativeButton(getString(R.string.quick_notes_file_name_dialog_negative_label)) { _, _ ->
                    createJSONFile(defaultFileName)
                }

                show()
            }
        }
    }

    private fun createJSONFile(fileName: String) {
        val fileSaveIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = "application/json"

            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, fileName)
        }

        fileSaveIntentLauncher.launch(fileSaveIntent)
    }

    private fun writeJSONFile(uri: Uri) {
        try {
            context?.contentResolver?.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { fileStream ->
                    val notesJSON = notesPreferenceManager.notesJSON

                    if (notesJSON != null) {
                        fileStream.write(notesJSON.toByteArray())
                    }
                }
            }
        } catch (ex: Exception) {
            Log.d("hub", "ex $ex")
        }
    }
}