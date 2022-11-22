package kenneth.app.starlightlauncher.noteswidget.util

import android.content.Context
import android.net.Uri

fun readBackupFile(uri: Uri, context: Context): String? =
    context.contentResolver.openInputStream(uri)
        ?.bufferedReader()
        ?.use { it.readText() }
