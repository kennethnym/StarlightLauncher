package kenneth.app.starlightlauncher.noteswidget.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kenneth.app.starlightlauncher.api.compose.LocalDataStore
import kenneth.app.starlightlauncher.api.compose.pref.ErrorDialog
import kenneth.app.starlightlauncher.api.compose.pref.SettingsList
import kenneth.app.starlightlauncher.api.compose.pref.SettingsListItem
import kenneth.app.starlightlauncher.api.compose.pref.SettingsScreen
import kenneth.app.starlightlauncher.noteswidget.R
import kenneth.app.starlightlauncher.noteswidget.util.readBackupFile
import java.io.FileOutputStream

private const val BACKUP_FILE_MIME_TYPE = "*/*"

@Composable
fun MainSettingsScreen() {
    val context = LocalContext.current
    val dataStore = LocalDataStore.current
    val viewModel: MainSettingsScreenViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainSettingsScreenViewModel(dataStore) as T
            }
        }
    )

    var backupFileUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var isExportDialogShown by remember {
        mutableStateOf(false)
    }

    val saveBackupFileIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.data?.let { uri ->
                    // Save the URI of the backup file for use with LaunchedEffect later
                    backupFileUri = uri
                    // Request to export the notes
                    viewModel.exportNotes()
                }
            }
        }
    )

    val pickBackupFileIntentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                readBackupFile(uri, context)?.let { json ->
                    viewModel.importNotes(json)
                }
            } ?: viewModel.backupCanceled()
        }
    )

    // open file picker ui to let user choose where to save the backup file
    fun openFilePicker(fileName: String) {
        saveBackupFileIntentLauncher.launch(
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                type = BACKUP_FILE_MIME_TYPE
                addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_TITLE, fileName)
            }
        )
    }

    // open file picker ui to let user choose the backup file to restore
    fun openBackupFilePicker() {
        viewModel.restoringBackup()
        pickBackupFileIntentLauncher.launch(BACKUP_FILE_MIME_TYPE)
    }

    fun writeToBackupFile(uri: Uri, json: String) {
        context.contentResolver.openFileDescriptor(uri, "w")?.use {
            FileOutputStream(it.fileDescriptor).use { stream ->
                stream.write(json.toByteArray())
            }
        }
        isExportDialogShown = false
        Toast.makeText(context, R.string.backup_successful_message, Toast.LENGTH_LONG)
            .show()
    }

    // Writes to the backup file when the notes are exported
    LaunchedEffect(viewModel.exportedNotes) {
        backupFileUri?.let {
            writeToBackupFile(it, viewModel.exportedNotes)
            backupFileUri = null
        }
    }

    // Notifies the user when the backup is restored
    LaunchedEffect(viewModel.isBackupRestored) {
        if (viewModel.isBackupRestored) {
            Toast.makeText(context, R.string.restore_successful_message, Toast.LENGTH_LONG)
                .show()
            viewModel.restoreMessageShown()
        }
    }

    SettingsScreen(
        title = stringResource(R.string.notes_widget_settings_title),
        description = stringResource(R.string.notes_widget_settings_description),
    ) {
        SettingsList {
            SettingsListItem(
                icon = painterResource(R.drawable.ic_export),
                title = stringResource(R.string.pref_title_export_to_json),
                summary = stringResource(R.string.pref_summary_export_to_json),
                onTap = { isExportDialogShown = true }
            )

            SettingsListItem(
                icon = painterResource(R.drawable.ic_history_alt),
                title = stringResource(R.string.pref_title_restore_from_backup),
                summary = stringResource(R.string.pref_summary_restore_from_backup),
                onTap = { openBackupFilePicker() },
                loading = viewModel.isRestoringBackup
            )
        }

        if (isExportDialogShown) {
            ExportNotesDialog(
                onDismissRequest = { isExportDialogShown = false },
                onSubmit = { openFilePicker(it) }
            )
        }

        viewModel.errorMessageRes?.let { (titleRes, messageRes) ->
            ErrorDialog(
                title = stringResource(titleRes),
                message = stringResource(messageRes),
                onDismissRequest = { viewModel.errorMessageShown() }
            )
        }
    }
}
