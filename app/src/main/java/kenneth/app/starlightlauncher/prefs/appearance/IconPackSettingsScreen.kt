package kenneth.app.starlightlauncher.prefs.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.IconPack
import kenneth.app.starlightlauncher.prefs.SettingsListItem
import kenneth.app.starlightlauncher.prefs.SettingsScreen
import kenneth.app.starlightlauncher.prefs.SettingsSection

@Composable
internal fun IconPackSettingsScreen(viewModel: IconPackSettingsScreenViewModel = viewModel()) {
    SettingsScreen(
        title = stringResource(R.string.appearance_change_icon_pack_title),
        description = stringResource(R.string.appearance_change_icon_pack_summary),
        gutter = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
            SelectedIconPackSection(iconPack = viewModel.selectedIconPack)
            InstalledIconPacksSection(iconPacks = viewModel.installedIconPacks)
        }
    }
}

@Composable
fun SelectedIconPackSection(iconPack: IconPack?) {
    SettingsSection(title = stringResource(R.string.appearance_current_icon_pack)) {
        if (iconPack == null) {
            Text(stringResource(R.string.status_loading))
        } else when (iconPack) {
            is DefaultIconPack ->
                Text(stringResource(R.string.appearance_no_current_icon_pack))
            is InstalledIconPack ->
                SettingsListItem(
                    icon = BitmapPainter(iconPack.icon.asImageBitmap()),
                    title = iconPack.name
                )
        }
    }
}

@Composable
internal fun InstalledIconPacksSection(
    iconPacks: List<InstalledIconPack>
) {
    SettingsSection(title = stringResource(R.string.appearance_installed_icon_pack)) {
        if (iconPacks.isEmpty())
            Text(
                stringResource(R.string.appearance_no_supported_icon_packs),
            )
        else
            Column(modifier = Modifier.padding(horizontal = 8.dp)) {
                iconPacks.forEach {
                    SettingsListItem(
                        icon = BitmapPainter(it.icon.asImageBitmap()),
                        title = it.name,
                    )
                }
            }
    }
}
