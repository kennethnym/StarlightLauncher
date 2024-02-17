package kenneth.app.starlightlauncher.noteswidget.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.pref.NotesWidgetPreferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AllNotesPageViewModel(launcher: StarlightLauncherApi) : ViewModel() {
    private val prefs = NotesWidgetPreferences.getInstance(launcher.dataStore)

    private val _notes by lazy {
        MutableLiveData<List<Note>>()
    }

    val notes: LiveData<List<Note>> = _notes

    init {
        viewModelScope.launch {
            prefs.notes.collectLatest {
                _notes.postValue(it)
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            prefs.deleteNote(note)
        }
    }

    fun editNote(note: Note) {
        viewModelScope.launch {
            prefs.editNote(note)
        }
    }

    fun addNote() {
        viewModelScope.launch {
            prefs.addNote(Note())
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val launcher = (this[VIEW_MODEL_STORE_OWNER_KEY] as AllNotesFragment).launcher
                AllNotesPageViewModel(launcher)
            }
        }
    }
}