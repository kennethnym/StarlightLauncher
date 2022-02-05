package kenneth.app.starlightlauncher.noteswidget.pref

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import kenneth.app.starlightlauncher.api.preference.ObservablePreferences
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.R
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal typealias NoteListListener = (event: NoteListChanged) -> Unit

internal data class NoteListChanged(
    val status: Status,
    val note: Note,
) {
    enum class Status {
        NOTE_ADDED,
        NOTE_CHANGED,
        NOTE_REMOVED,
    }
}

internal class NotesWidgetPreferences
private constructor(context: Context) :
    ObservablePreferences<NotesWidgetPreferences>(context) {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: NotesWidgetPreferences? = null

        fun getInstance(context: Context) =
            instance ?: NotesWidgetPreferences(context.applicationContext)
                .also { instance = it }
    }

    private val noteList = mutableListOf<Note>()

    val keys = PrefKeys(context)

    val notes
        get() = noteList.toList()

    init {
        loadNotes()
    }

    override fun updateValue(sharedPreferences: SharedPreferences, key: String) {}

    internal fun addNote(note: Note) {
        noteList += note
        sharedPreferences.edit(commit = true) {
            putString(keys.noteList, Json.encodeToString(noteList))
        }
        notifyNoteAdded(note)
    }

    internal fun editNote(note: Note) {
        val i = noteList.indexOfFirst { it.id == note.id }
        noteList[i] = note
        sharedPreferences.edit(commit = true) {
            putString(keys.noteList, Json.encodeToString(noteList))
        }
        notifyNoteEdited(note)
    }

    internal fun deleteNote(note: Note) {
        Log.d("Starlight", "Deleting note, id ${note.id}")
        Log.d("Starlight", "Current notes ${noteList.size}")
        val index = noteList.indexOf(note)
        noteList.removeAt(index)
        Log.d("Starlight", "Index $index")
        sharedPreferences.edit(commit = true) {
            putString(keys.noteList, Json.encodeToString(noteList))
        }
        notifyNoteRemoved(note)
    }

    internal fun addNoteListChangedListener(listener: NoteListListener) {
        addObserver { o, arg ->
            if (arg is NoteListChanged) listener(arg)
        }
    }

    private fun notifyNoteAdded(noteAdded: Note) {
        setChanged()
        notifyObservers(
            NoteListChanged(
                status = NoteListChanged.Status.NOTE_ADDED,
                note = noteAdded,
            )
        )
    }

    private fun notifyNoteRemoved(noteRemoved: Note) {
        setChanged()
        notifyObservers(
            NoteListChanged(
                status = NoteListChanged.Status.NOTE_REMOVED,
                note = noteRemoved
            )
        )
    }

    private fun notifyNoteEdited(noteEdited: Note) {
        setChanged()
        notifyObservers(
            NoteListChanged(
                status = NoteListChanged.Status.NOTE_CHANGED,
                note = noteEdited
            )
        )
    }

    private fun loadNotes() {
        sharedPreferences.getString(keys.noteList, null)?.let {
            val notes = Json.decodeFromString<List<Note>>(it)
            noteList += notes
        }
    }
}

internal class PrefKeys(context: Context) {
    val noteList by lazy {
        context.getString(R.string.pref_key_note_list)
    }
}