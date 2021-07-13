package kenneth.app.spotlightlauncher.prefs.notes

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.models.Note
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

typealias NoteListListener = (notes: List<Note>) -> Unit

/**
 * Manages preferences related to notes
 */
@Singleton
class NotesPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sharedPreferences: SharedPreferences
) {
    var notesJson: String?
        private set

    val notes: MutableList<Note>

    private var noteListListeners = mutableListOf<NoteListListener>()

    private val noteListPrefKey by lazy { context.getString(R.string.note_list) }

    init {
        notesJson = sharedPreferences.getString(noteListPrefKey, null)
        notes = notesJson
            ?.let {
                Json.decodeFromString<List<Note>>(it)
                    .toMutableList()
            }
            ?: mutableListOf()
    }

    /**
     * Registers the given [listener] as a listener of the note list.
     * Whenver the list changes, [listener] is called.
     */
    fun addNoteListChangedListener(listener: NoteListListener) {
        noteListListeners.add(listener)
    }

    /**
     * Saves the edited note.
     * @param note The edited note. The id has to be identical with the id of the original note.
     */
    fun editNote(note: Note) {
        val i = notes.indexOfFirst { it.id == note.id }
        notes[i] = note
        notesJson = Json.encodeToString(notes)

        sharedPreferences
            .edit()
            .putString(noteListPrefKey, notesJson)
            .apply()

        notifyListeners()
    }

    /**
     * Adds and saves the given [note].
     * @param note The new [Note] to add
     */
    fun addNote(note: Note) {
        Log.d("hub", "note $note")

        notes += note
        notesJson = Json.encodeToString(notes)

        sharedPreferences
            .edit()
            .putString(noteListPrefKey, notesJson)
            .apply()

        notifyListeners()
    }

    /**
     * Deletes the given [note].
     * @param note The [Note] to delete
     */
    fun deleteNote(note: Note) {
        notes.removeIf { it == note }
        notesJson = Json.encodeToString(notes)

        sharedPreferences
            .edit()
            .putString(noteListPrefKey, notesJson)
            .apply()

        notifyListeners()
    }

    /**
     * Restores notes from the given json.
     * @param json The JSON string that contains a list of notes to be restored
     * @throws kotlinx.serialization.SerializationException when the JSON is invalid or is not
     * in a correct shape.
     */
    fun restoreNotesFromJSON(json: String) {
        val restoredNotes = Json.decodeFromString<List<Note>>(json)
        notesJson = json
        notes.addAll(restoredNotes)
    }

    private fun notifyListeners() {
        noteListListeners.forEach { it(notes) }
    }
}