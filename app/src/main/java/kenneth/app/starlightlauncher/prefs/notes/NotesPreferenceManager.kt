package kenneth.app.starlightlauncher.prefs.notes

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.spotlightlauncher.R
import kenneth.app.starlightlauncher.models.Note
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

    val notes: List<Note>
        get() = notesMap.values.toList()

    private val notesMap = mutableMapOf<String, Note>()

    private var noteListListeners = mutableListOf<NoteListListener>()

    private val noteListPrefKey by lazy { context.getString(R.string.note_list) }

    init {
        notesJson = sharedPreferences.getString(noteListPrefKey, null)
        notesJson?.let { json ->
            Json.decodeFromString<List<Note>>(json)
                .forEach { notesMap.putIfAbsent(it.id, it) }
        }
    }

    /**
     * Registers the given [listener] as a listener of the note list.
     * Whenever the list changes, [listener] is called.
     */
    fun addNoteListChangedListener(listener: NoteListListener) {
        noteListListeners.add(listener)
    }

    fun removeNoteListChangedListener(listener: NoteListListener) {
        noteListListeners.remove(listener)
    }

    /**
     * Saves the edited note.
     * @param note The edited note. The id has to be identical with the id of the original note.
     */
    fun editNote(note: Note) {
        notesMap[note.id] = note
        notesJson = Json.encodeToString(notes)

        saveNotesToStorage()
        notifyListeners()
    }

    /**
     * Adds and saves the given [note].
     * @param note The new [Note] to add
     */
    fun addNote(note: Note) {
        Log.d("hub", "note $note")

        notesMap[note.id] = note
        notesJson = Json.encodeToString(notes)

        saveNotesToStorage()
        notifyListeners()
    }

    /**
     * Deletes the given [note].
     * @param note The [Note] to delete
     */
    fun deleteNote(note: Note) {
        notesMap.remove(note.id)
        notesJson = Json.encodeToString(notes)

        saveNotesToStorage()
        notifyListeners()
    }

    /**
     * Restores notes from the given json.
     * @param json The JSON string that contains a list of notes to be restored
     * @throws kotlinx.serialization.SerializationException when the JSON is invalid or is not
     * in a correct shape.
     */
    fun restoreNotesFromJSON(json: String) {
        notesJson = json
        Json.decodeFromString<List<Note>>(json)
            .forEach { notesMap.putIfAbsent(it.id, it) }
        saveNotesToStorage()
        notifyListeners()
    }

    private fun notifyListeners() {
        noteListListeners.forEach { it(notes) }
    }

    private fun saveNotesToStorage() {
        sharedPreferences
            .edit()
            .putString(noteListPrefKey, notesJson)
            .apply()
    }
}