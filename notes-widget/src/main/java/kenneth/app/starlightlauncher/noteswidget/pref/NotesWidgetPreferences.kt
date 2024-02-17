package kenneth.app.starlightlauncher.noteswidget.pref

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.PREF_KEY_NOTE_LIST
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class NotesWidgetPreferences
private constructor(private val dataStore: DataStore<Preferences>) {
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
            if (it.contains(PREF_KEY_NOTE_LIST)) {
                it[PREF_KEY_NOTE_LIST] = Json.encodeToString(notes.first() + note)
            } else {
                it[PREF_KEY_NOTE_LIST] = Json.encodeToString(
                    listOf(note)
                )
            }
        }
    }

    suspend fun editNote(note: Note) {
        dataStore.edit { preferences ->
            preferences[PREF_KEY_NOTE_LIST] = Json.encodeToString(
                notes.first().map { if (it.id == note.id) note else it }
            )
        }
    }

    suspend fun deleteNote(note: Note) {
        dataStore.edit { preferences ->
            preferences[PREF_KEY_NOTE_LIST] = Json.encodeToString(
                notes.first().filter { it != note }
            )
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
