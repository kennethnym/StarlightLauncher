package kenneth.app.starlightlauncher.noteswidget.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kenneth.app.starlightlauncher.noteswidget.R
import kenneth.app.starlightlauncher.noteswidget.pref.NotesWidgetPreferences
import kotlinx.coroutines.launch

class MainSettingsScreenViewModel(dataStore: DataStore<Preferences>) : ViewModel() {
    private val prefs = NotesWidgetPreferences.getInstance(dataStore)

    var isBackupRestored by mutableStateOf(false)
        private set

    var isRestoringBackup by mutableStateOf(false)
        private set

    var errorMessageRes by mutableStateOf<Pair<Int, Int>?>(null)
        private set

    var exportedNotes by mutableStateOf("")
        private set

    fun exportNotes() {
        viewModelScope.launch {
            exportedNotes = prefs.exportNotesToJson()
        }
    }

    fun importNotes(json: String) {
        viewModelScope.launch {
            try {
                prefs.restoreNotesFromJson(json)
                isBackupRestored = true
                isRestoringBackup = false
            } catch (e: Exception) {
                errorMessageRes = Pair(
                    R.string.restore_failed_dialog_title,
                    R.string.restore_failed_dialog_message
                )
                isBackupRestored = false
                isRestoringBackup = false
            }
        }
    }

    fun errorMessageShown() {
        errorMessageRes = null
    }

    fun restoringBackup() {
        isRestoringBackup = true
    }

    fun backupCanceled() {
        isRestoringBackup = false
    }

    fun restoreMessageShown() {
        isBackupRestored = false
    }
}
