package kenneth.app.starlightlauncher.filesearchmodule.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kenneth.app.starlightlauncher.api.compose.LocalDataStore
import kenneth.app.starlightlauncher.api.compose.pref.SettingsList
import kenneth.app.starlightlauncher.api.compose.pref.SettingsListItem
import kenneth.app.starlightlauncher.api.compose.pref.SettingsScreen
import kenneth.app.starlightlauncher.filesearchmodule.DocumentProviderName
import kenneth.app.starlightlauncher.filesearchmodule.R

val DOCUMENT_PROVIDER_LABEL = mapOf<String, Int>(
    DocumentProviderName.EXTERNAL.authority to R.string.document_provider_external_storage
)

@Composable
fun MainSettingsScreen() {
    val context = LocalContext.current

    val dataStore = LocalDataStore.current
    val viewModel: MainSettingsScreenViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainSettingsScreenViewModel(dataStore) as T
            }
        }
    )

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) {
        if (it != null) {
            context.contentResolver.takePersistableUriPermission(
                it,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.addSearchPath(it)
        }
    }

    fun openFilePicker() {
        filePicker.launch(Uri.EMPTY)
    }

    SettingsScreen(
        title = stringResource(R.string.file_search_module_search_module_settings_title),
        description = stringResource(R.string.file_search_module_settings_screen_description),
    ) {
        SettingsList {
            viewModel.includedSearchPaths.forEach { searchPathUri ->
                val path = searchPathUri.path?.split(":")?.get(1)
                SettingsListItem(
                    title = path ?: "/",
                    summary = DocumentProviderName.fromUri(searchPathUri)?.displayName?.let {
                        stringResource(it)
                    }
                )
            }

            SettingsListItem(
                title = stringResource(R.string.add_path_label),
                icon = painterResource(R.drawable.ic_plus),
                onTap = { openFilePicker() }
            )
        }
    }
}
