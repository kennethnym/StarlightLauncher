package kenneth.app.starlightlauncher.datetime

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.compose.pref.SETTINGS_LIST_ITEM_ICON_SIZE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherApiKeyDialog(
    onDismissRequest: () -> Unit,
    onApiKeySelected: (apiKey: String) -> Unit,
    onDefaultApiKeySelected: () -> Unit,
) {
    var apiKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Image(
                painter = painterResource(R.drawable.ic_key_skeleton),
                contentDescription = "",
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .width(SETTINGS_LIST_ITEM_ICON_SIZE)
                    .height(SETTINGS_LIST_ITEM_ICON_SIZE),
            )
        },
        title = {
            Text(stringResource(R.string.date_time_custom_api_key_dialog_title))
        },
        text = {
            TextField(
                value = apiKey,
                onValueChange = { apiKey = it },
            )
        },
        confirmButton = {
            TextButton(onClick = { onApiKeySelected(apiKey) }) {
                Text(stringResource(R.string.date_time_custom_api_key_dialog_confirm_button))
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        onDefaultApiKeySelected()
                        onDismissRequest()
                    }
                ) {
                    Text(stringResource(R.string.date_time_custom_api_key_dialog_use_default_key_button))
                }

                TextButton(onClick = { onDismissRequest() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        },
    )
}