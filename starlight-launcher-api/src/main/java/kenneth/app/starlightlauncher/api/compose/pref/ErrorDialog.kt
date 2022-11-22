package kenneth.app.starlightlauncher.api.compose.pref

import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kenneth.app.starlightlauncher.api.R

@Composable
fun ErrorDialog(
    title: String,
    message: String,
    onDismissRequest: () -> Unit,
) {
    AlertDialog(
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_exclamation_triangle),
                contentDescription = "",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.error),
            )
        },
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { onDismissRequest() }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        onDismissRequest = onDismissRequest
    )
}
