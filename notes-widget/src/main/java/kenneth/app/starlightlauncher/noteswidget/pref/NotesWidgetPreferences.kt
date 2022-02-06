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

internal typealias NoteListListener = (event: NoteListModified) -> Unit

sealed class NoteListModified {
    data class ListChanged(val notes: List<Note>) : NoteListModified()
    data class NoteAdded(val note: Note) : NoteListModified()
    data class NoteChanged(val note: Note) : NoteListModified()
    data class NoteRemoved(val note: Note) : NoteListModified()
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
        saveNoteList()
        notifyNoteAdded(note)
    }

    internal fun editNote(note: Note) {
        val i = noteList.indexOfFirst { it.id == note.id }
        noteList[i] = note
        saveNoteList()
        notifyNoteEdited(note)
    }

    internal fun deleteNote(note: Note) {
        Log.d("Starlight", "Deleting note, id ${note.id}")
        Log.d("Starlight", "Current notes ${noteList.size}")
        val index = noteList.indexOf(note)
        noteList.removeAt(index)
        Log.d("Starlight", "Index $index")
        saveNoteList()
        notifyNoteRemoved(note)
    }

    internal fun addNoteListModifiedListener(listener: NoteListListener) {
        addObserver { o, arg ->
            if (arg is NoteListModified) listener(arg)
        }
    }

    internal fun exportNotesToJson(): String = Json.encodeToString(notes)

    internal fun restoreNotesFromJson(json: String) {
        noteList.clear()
        noteList += Json.decodeFromString<List<Note>>(json)
        saveNoteList()
        notifyNoteListChanged()
    }

    private fun notifyNoteListChanged() {
        setChanged()
        notifyObservers(NoteListModified.ListChanged(notes))
    }

    private fun notifyNoteAdded(noteAdded: Note) {
        setChanged()
        notifyObservers(NoteListModified.NoteAdded(noteAdded))
    }

    private fun notifyNoteRemoved(noteRemoved: Note) {
        setChanged()
        notifyObservers(NoteListModified.NoteRemoved(noteRemoved))
    }

    private fun notifyNoteEdited(noteEdited: Note) {
        setChanged()
        notifyObservers(NoteListModified.NoteChanged(noteEdited))
    }

    private fun loadNotes() {
        sharedPreferences.getString(keys.noteList, null)?.let {
            val notes = Json.decodeFromString<List<Note>>(it)
            noteList += notes
        }
    }

    private fun saveNoteList() {
        sharedPreferences.edit(commit = true) {
            putString(keys.noteList, Json.encodeToString(noteList))
        }
    }
}

internal class PrefKeys(context: Context) {
    val noteList by lazy {
        context.getString(R.string.pref_key_note_list)
    }
}