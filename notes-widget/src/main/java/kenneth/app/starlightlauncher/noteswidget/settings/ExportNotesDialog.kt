package kenneth.app.starlightlauncher.noteswidget.settings

import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kenneth.app.starlightlauncher.noteswidget.R
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportNotesDialog(
    onDismissRequest: () -> Unit,
    onSubmit: (fileName: String) -> Unit
) {
    val backupFileTimestampFormat = SimpleDateFormat("Md_y_kms", Locale.getDefault())
    val defaultFileName =
        stringResource(R.string.backup_file_prefix) + backupFileTimestampFormat.format(Date())

    var fileName by remember { mutableStateOf(defaultFileName) }
    var errorMessageRes by remember { mutableStateOf<Int?>(null) }

    AlertDialog(
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_export),
                contentDescription = "",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
            )
        },
        title = {
            Text(stringResource(R.string.export_dialog_title))
        },
        text = {
            TextField(
                value = fileName,
                onValueChange = { fileName = it },
                supportingText = {
                    errorMessageRes?.let { Text(stringResource(it)) }
                },
                isError = errorMessageRes != null,
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (fileName.isBlank()) {
                        errorMessageRes = R.string.export_dialog_empty_name_error
                    } else {
                        onSubmit(fileName)
                    }
                }
            ) {
                Text(stringResource(R.string.action_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        onDismissRequest = onDismissRequest
    )
}