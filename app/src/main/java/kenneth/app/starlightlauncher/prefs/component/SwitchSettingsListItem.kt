package kenneth.app.starlightlauncher.prefs.component

import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import kenneth.app.starlightlauncher.R

@Composable
fun SwitchSettingsListItem(
    icon: Painter? = null,
    emptyIcon: Boolean = icon == null,
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (value: Boolean) -> Unit
) {
    SettingsListItem(
        icon = icon,
        emptyIcon = emptyIcon,
        title = title,
        summary = summary,
        control = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
        onTap = {
            onCheckedChange(!checked)
        }
    )
}
