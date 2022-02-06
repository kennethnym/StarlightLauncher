package kenneth.app.starlightlauncher.noteswidget.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kenneth.app.starlightlauncher.noteswidget.R
import java.text.SimpleDateFormat
import java.util.*

class WidgetSettingsFragment : PreferenceFragmentCompat() {
    private val backupFileTimestampFormat = SimpleDateFormat("Md_y_kms", Locale.getDefault())

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.notes_widget_root_preferences, rootKey)

        findPreference<Preference>(getString(R.string.pref_key_export_to_json))
            ?.setOnPreferenceClickListener {
                exportNotes()
                true
            }

        findPreference<Preference>(getString(R.string.pref_key_restore_from_backup))
            ?.setOnPreferenceClickListener {
                true
            }
    }

    private fun exportNotes() {
        showFileNameDialog()
    }

    private fun showFileNameDialog() {
        context?.let { context ->
            val defaultFileName =
                getString(R.string.backup_file_prefix) + backupFileTimestampFormat.format(Date())

            val fileNameEditText = TextInputEditText(context)
            val fileNameEditTextLayout = TextInputLayout(
                context,
                null,
                R.style.Widget_Material3_TextInputLayout_FilledBox,
            ).apply {
                hint = "test"
                addView(fileNameEditText)
            }

            MaterialAlertDialogBuilder(context).run {
                setTitle(getString(R.string.file_name_dialog_title))
                setMessage(getString(R.string.file_name_dialog_message, defaultFileName))

                setView(fileNameEditTextLayout)

                setPositiveButton(getString(R.string.file_name_dialog_positive_label)) { _, _ ->

                }

                setNegativeButton(getString(R.string.file_name_dialog_negative_label)) { _, _ ->

                }

                show()
            }
        }
    }
}