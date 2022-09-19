package kenneth.app.starlightlauncher.noteswidget.fragment

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kenneth.app.starlightlauncher.noteswidget.R
import kenneth.app.starlightlauncher.noteswidget.databinding.FileNameDialogContentBinding
import kenneth.app.starlightlauncher.noteswidget.pref.NotesWidgetPreferences
import java.io.FileOutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

private const val BACKUP_FILE_MIME_TYPE = "*/*"

class WidgetSettingsFragment : PreferenceFragmentCompat() {
    private val backupFileTimestampFormat = SimpleDateFormat("Md_y_kms", Locale.getDefault())

    private val saveBackupFileIntentLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::writeToBackupFile
        )

    private val restoreBackupIntentLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent(), ::handlePickedBackupFile)

    private var prefs: NotesWidgetPreferences? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.notes_widget_root_preferences, rootKey)

        findPreference<Preference>(getString(R.string.pref_key_export_to_json))
            ?.setOnPreferenceClickListener {
                exportNotes()
                true
            }

        findPreference<Preference>(getString(R.string.pref_key_restore_from_backup))
            ?.setOnPreferenceClickListener {
                restoreBackupIntentLauncher.launch(BACKUP_FILE_MIME_TYPE)
                true
            }

//        prefs = context?.let { NotesWidgetPreferences.getInstance(it) }
    }

    private fun exportNotes() {
        showFileNameDialog()
    }

    private fun showFileNameDialog() {
        context?.let { context ->
            val defaultFileName =
                getString(R.string.backup_file_prefix) + backupFileTimestampFormat.format(Date())

            val contentBinding = FileNameDialogContentBinding.inflate(LayoutInflater.from(context))

            MaterialAlertDialogBuilder(
                context,
                R.style.ThemeOverlay_Material3_MaterialAlertDialog_Centered
            ).run {
                setIcon(R.drawable.ic_export)
                setTitle(getString(R.string.file_name_dialog_title))
                setMessage(getString(R.string.file_name_dialog_message, defaultFileName))

                setView(contentBinding.root)

                setPositiveButton(getString(R.string.file_name_dialog_positive_label)) { _, _ ->
                    createBackupFile(contentBinding.fileNameEditText.text.toString())
                }

                setNegativeButton(getString(R.string.file_name_dialog_negative_label)) { _, _ ->
                    createBackupFile(defaultFileName)
                }

                show()
            }
        }
    }

    private fun createBackupFile(name: String) {
        Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            type = BACKUP_FILE_MIME_TYPE
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_TITLE, name)
        }.run {
            saveBackupFileIntentLauncher.launch(this)
        }
    }

    private fun writeToBackupFile(activityResult: ActivityResult) {
        if (activityResult.resultCode == Activity.RESULT_OK) {
            try {
                activityResult.data?.data?.let { uri ->
                    context?.contentResolver?.openFileDescriptor(uri, "w")?.use {
                        FileOutputStream(it.fileDescriptor).use { stream ->
                            prefs?.exportNotesToJson()?.let { json ->
                                stream.write(json.toByteArray())
                            }
                        }
                    }
                }
                showBackupSuccessConfirmation()
            } catch (ex: Exception) {

            }
        }
    }

    private fun showBackupSuccessConfirmation() {
        Snackbar
            .make(
                requireView(),
                getString(R.string.backup_successful_message),
                Snackbar.LENGTH_SHORT
            )
            .show()
    }

    private fun showRestoreSuccessConfirmation() {
        Snackbar
            .make(
                requireView(),
                getString(R.string.restore_successful_message),
                Snackbar.LENGTH_SHORT
            )
            .show()
    }

    private fun handlePickedBackupFile(fileUri: Uri?) {
        if (fileUri != null) {
            context?.contentResolver?.openInputStream(fileUri)
                ?.bufferedReader()
                ?.use { it.readText() }
                ?.let { restoreNotes(it) }
        }
    }

    private fun restoreNotes(json: String) {
//        prefs?.let { prefs ->
//            prefs.restoreNotesFromJson(json)
//            showRestoreSuccessConfirmation()
//        }
    }
}