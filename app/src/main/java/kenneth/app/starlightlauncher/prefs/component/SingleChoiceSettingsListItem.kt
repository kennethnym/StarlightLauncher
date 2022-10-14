package kenneth.app.starlightlauncher.prefs.component

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.Text
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kenneth.app.starlightlauncher.R

@Composable
fun <T> SingleChoiceSettingsListItem(
    labels: Iterable<String>,
    values: Iterable<T>,
    choice: T?,
    icon: Painter? = null,
    title: String,
    summary: String? = null,
    onChoiceSelected: (value: T) -> Unit
) {
    var isDropdownOpen by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxWidth()
            .wrapContentSize(Alignment.TopStart)
    ) {
        SettingsListItem(
            icon = icon,
            title = title,
            summary = summary
                ?: choice?.let { labels.elementAt(values.indexOf(it)) }
                ?: "Not selected",
            control = {
                Image(
                    painter = painterResource(
                        if (isDropdownOpen) R.drawable.ic_angle_up
                        else R.drawable.ic_angle_down
                    ),
                    contentDescription = "Dropdown indicator"
                )
            },
            onTap = { isDropdownOpen = true }
        )

        DropdownMenu(
            expanded = isDropdownOpen,
            onDismissRequest = { isDropdownOpen = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            labels.zip(values).forEach { (choiceLabel, choiceValue) ->
                DropdownMenuItem(
                    text = {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (choiceValue == choice)
                                Image(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = stringResource(R.string.status_selected),
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(24.dp)
                                )
                            else
                                Box(
                                    Modifier
                                        .width(24.dp)
                                        .height(24.dp)
                                )

                            Text(choiceLabel)
                        }
                    },
                    onClick = {
                        isDropdownOpen = false
                        onChoiceSelected(choiceValue)
                    }
                )
            }
        }
    }
}