package kenneth.app.starlightlauncher.prefs

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kenneth.app.starlightlauncher.extension.Extension
import kenneth.app.starlightlauncher.extension.ExtensionManager
import javax.inject.Inject

@HiltViewModel
internal class RootSettingsScreenViewModel @Inject constructor(
    extensionManager: ExtensionManager
) : ViewModel() {
    var installedExtensions by mutableStateOf<List<Extension>>(emptyList())

    init {
        installedExtensions = extensionManager.installedExtensions.toList()
    }
}