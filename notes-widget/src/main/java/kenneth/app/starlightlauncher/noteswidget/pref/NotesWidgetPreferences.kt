package kenneth.app.starlightlauncher.noteswidget.pref

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kenneth.app.starlightlauncher.api.util.EventChannel
import kenneth.app.starlightlauncher.noteswidget.Note
import kenneth.app.starlightlauncher.noteswidget.R
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
private constructor(context: Context) : EventChannel<NoteListModified>() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private var instance: NotesWidgetPreferences? = null

        fun getInstance(context: Context) =
            instance ?: NotesWidgetPreferences(context.applicationContext)
                .also { instance = it }
    }

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val noteList = mutableListOf<Note>()

    private val keys = PrefKeys(context)

    val notes
        get() = noteList.toList()

    init {
        loadNotes()
    }

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

    internal fun exportNotesToJson(): String = Json.encodeToString(notes)

    internal fun restoreNotesFromJson(json: String) {
        noteList.clear()
        noteList += Json.decodeFromString<List<Note>>(json)
        saveNoteList()
        notifyNoteListChanged()
    }

    private fun notifyNoteListChanged() {
        add(NoteListModified.ListChanged(notes))
    }

    private fun notifyNoteAdded(noteAdded: Note) {
        add(NoteListModified.NoteAdded(noteAdded))
    }

    private fun notifyNoteRemoved(noteRemoved: Note) {
        add(NoteListModified.NoteRemoved(noteRemoved))
    }

    private fun notifyNoteEdited(noteEdited: Note) {
        add(NoteListModified.NoteChanged(noteEdited))
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