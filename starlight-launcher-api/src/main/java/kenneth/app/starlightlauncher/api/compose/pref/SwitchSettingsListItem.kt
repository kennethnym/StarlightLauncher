package kenneth.app.starlightlauncher.api.compose.pref

import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

@Composable
fun SwitchSettingsListItem(
    icon: Painter? = null,
    emptyIcon: Boolean = icon == null,
    enabled: Boolean = true,
    title: String,
    summary: String? = null,
    checked: Boolean,
    onCheckedChange: (value: Boolean) -> Unit
) {
    SettingsListItem(
        icon = icon,
        emptyIcon = emptyIcon,
        enabled = enabled,
        title = title,
        summary = summary,
        control = {
            Switch(
                enabled = enabled,
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
        onTap = {
            onCheckedChange(!checked)
        }
    )
}
