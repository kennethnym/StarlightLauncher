package kenneth.app.starlightlauncher.prefs.appearance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.IconPack
import kenneth.app.starlightlauncher.api.compose.pref.SettingsListItem
import kenneth.app.starlightlauncher.api.compose.pref.SettingsScreen
import kenneth.app.starlightlauncher.api.compose.pref.SettingsSection

@Composable
internal fun IconPackSettingsScreen(viewModel: IconPackSettingsScreenViewModel = hiltViewModel()) {
    SettingsScreen(
        title = stringResource(R.string.appearance_change_icon_pack_title),
        description = stringResource(R.string.appearance_change_icon_pack_summary),
        gutter = true,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
            SelectedIconPackSection(iconPack = viewModel.selectedIconPack)
            InstalledIconPacksSection(
                iconPacks = viewModel.installedIconPacks,
                currentIconPack = viewModel.selectedIconPack,
            ) {
                viewModel.changeIconPack(it)
            }

            Divider()

            SettingsListItem(
                icon = painterResource(R.drawable.ic_history_alt),
                title = stringResource(R.string.appearance_revert_icon_pack_title),
                summary = stringResource(id = R.string.appearance_revert_icon_pack_summary)
            )
        }
    }
}

@Composable
fun SelectedIconPackSection(
    iconPack: IconPack?,
) {
    SettingsSection(title = stringResource(R.string.appearance_current_icon_pack)) {
        when (iconPack) {
            null -> Text(
                stringResource(R.string.status_loading),
                style = MaterialTheme.typography.bodySmall
            )
            is DefaultIconPack ->
                Text(
                    stringResource(R.string.appearance_no_current_icon_pack),
                    style = MaterialTheme.typography.bodySmall
                )
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
    iconPacks: List<InstalledIconPack>,
    currentIconPack: IconPack?,
    onChangeIconPack: (iconPack: InstalledIconPack) -> Unit
) {
    val availableIconPacks = iconPacks.filter {
        if (currentIconPack is InstalledIconPack)
            it.packageName != currentIconPack.packageName
        else true
    }

    SettingsSection(title = stringResource(R.string.appearance_installed_icon_pack)) {
        if (iconPacks.isEmpty() || availableIconPacks.isEmpty())
            Text(
                stringResource(R.string.appearance_no_supported_icon_packs),
                style = MaterialTheme.typography.bodySmall
            )
        else
            Column {
                availableIconPacks.forEach {
                    SettingsListItem(
                        icon = BitmapPainter(it.icon.asImageBitmap()),
                        title = it.name,
                        onTap = { onChangeIconPack(it) }
                    )
                }
            }
    }
}
