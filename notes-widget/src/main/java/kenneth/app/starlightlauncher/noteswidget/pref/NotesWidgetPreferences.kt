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

    private val notesMap = mutableMapOf<String, Note>()
    private val noteList = mutableListOf<Note>()
    private var notesCount = 0

    val keys = PrefKeys(context)

    val notes
        get() = noteList.toList()

    init {
        loadNotes()
    }

    override fun updateValue(sharedPreferences: SharedPreferences, key: String) {}

    fun addNote(note: Note) {
        notesMap[note.id] = note
        noteList += note
        notesCount += 1
        sharedPreferences.edit(commit = true) {
            putInt(keys.notesCount, notesCount)
            putString(keys.noteList + (notesCount - 1), Json.encodeToString(note))
        }
        notifyNoteAdded(note)
    }

    fun deleteNote(note: Note) {
        Log.d("Starlight", "Deleting note, id ${note.id}")
        Log.d("Starlight", "Current notes ${noteList.size}")
        notesMap.remove(note.id)
        val index = noteList.indexOf(note)
        noteList.removeAt(index)
        Log.d("Starlight", "Index $index")
        notesCount -= 1
        sharedPreferences.edit(commit = true) {
            putInt(keys.notesCount, notesCount)
            remove(keys.noteList + index)
        }
        notifyNoteRemoved(note)
    }

    fun getNoteAt(index: Int) = noteList[index]

    fun addNoteListChangedListener(listener: NoteListListener) {
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

    private fun loadNotes() {
        notesCount = sharedPreferences.getInt(keys.notesCount, 0)
        for (i in 0 until notesCount) {
            sharedPreferences.getString(keys.noteList + i, null)?.let { json ->
                val note = Json.decodeFromString<Note>(json)
                notesMap[note.id] = note
                noteList += note
            }
        }
    }
}

internal class PrefKeys(context: Context) {
    val noteList by lazy {
        context.getString(R.string.pref_key_note_list)
    }

    val notesCount by lazy {
        context.getString(R.string.pref_key_notes_count)
    }
}