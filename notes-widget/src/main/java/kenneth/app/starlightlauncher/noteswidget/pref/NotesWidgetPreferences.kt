package kenneth.app.starlightlauncher.noteswidget.pref

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kenneth.app.starlightlauncher.api.util.EventChannel
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.PREF_KEY_NOTE_LIST
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal sealed class NoteListModified {
    data class ListChanged(val notes: List<Note>) : NoteListModified()
    data class NoteAdded(val note: Note) : NoteListModified()
    data class NoteChanged(val note: Note) : NoteListModified()
    data class NoteRemoved(val note: Note) : NoteListModified()
}

internal class NotesWidgetPreferences
private constructor(private val dataStore: DataStore<Preferences>) :
    EventChannel<NoteListModified>() {
    companion object {
        private var instance: NotesWidgetPreferences? = null

        fun getInstance(dataStore: DataStore<Preferences>) =
            instance ?: NotesWidgetPreferences(dataStore)
                .also { instance = it }
    }

    val notes = dataStore.data.map { preferences ->
        preferences[PREF_KEY_NOTE_LIST]?.let {
            Json.decodeFromString<List<Note>>(it)
        } ?: emptyList()
    }

    suspend fun addNote(note: Note) {
        dataStore.edit {
            it[PREF_KEY_NOTE_LIST]?.let { json ->
                val notes = Json.decodeFromString<List<Note>>(json)
                it[PREF_KEY_NOTE_LIST] = Json.encodeToString(notes + note)
            } ?: kotlin.run {
                it[PREF_KEY_NOTE_LIST] = Json.encodeToString(
                    listOf(note)
                )
            }
        }
    }

    suspend fun editNote(note: Note) {
        dataStore.edit { preferences ->
            preferences[PREF_KEY_NOTE_LIST]?.let { json ->
                val notes = Json.decodeFromString<List<Note>>(json).toMutableList()
                preferences[PREF_KEY_NOTE_LIST] = Json.encodeToString(
                    notes.map { if (it.id == note.id) note else it }
                )
            }
        }
    }

    suspend fun deleteNote(note: Note) {
        dataStore.edit { preferences ->
            preferences[PREF_KEY_NOTE_LIST]?.let { json ->
                val notes = Json.decodeFromString<List<Note>>(json)
                preferences[PREF_KEY_NOTE_LIST] = Json.encodeToString(
                    notes.filter { it != note }
                )
            }
        }
    }

    suspend fun exportNotesToJson(): String = Json.encodeToString(notes.first())

    suspend fun restoreNotesFromJson(json: String) {
        val restoredNotes = Json.decodeFromString<List<Note>>(json)
        dataStore.edit {
            it[PREF_KEY_NOTE_LIST] = Json.encodeToString(restoredNotes)
        }
    }
}
