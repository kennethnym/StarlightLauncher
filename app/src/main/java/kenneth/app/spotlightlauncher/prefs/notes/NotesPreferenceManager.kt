package kenneth.app.spotlightlauncher.prefs.notes

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.models.Note
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
    val notesJSON: String?
        get() = sharedPreferences.getString(noteListPrefKey, null)

    val notes: List<Note>
        get() {
            val listStr = notesJSON
            return if (listStr != null) Json.decodeFromString(listStr) else emptyList()
        }

    private var noteListListener: NoteListListener? = null

    private val noteListPrefKey by lazy { context.getString(R.string.note_list) }

    /**
     * Registers the given [listener] as a listener of the note list.
     * Whenver the list changes, [listener] is called.
     */
    fun setOnNoteListChangedListener(listener: NoteListListener) {
        noteListListener = listener
    }

    /**
     * Adds and saves the given [note].
     * @param note The new [Note] to add
     */
    fun addNote(note: Note) {
        val newList = notes + note

        sharedPreferences
            .edit()
            .putString(noteListPrefKey, Json.encodeToString(newList))
            .apply()

        noteListListener?.let { it(newList) }
    }

    /**
     * Deletes the given [note].
     * @param note The [Note] to delete
     */
    fun deleteNote(note: Note) {
        val newList = notes.filter { it != note }

        sharedPreferences
            .edit()
            .putString(noteListPrefKey, Json.encodeToString(newList))
            .apply()

        noteListListener?.let { it(newList) }
    }
}