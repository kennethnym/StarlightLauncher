package kenneth.app.starlightlauncher.filesearchmodule.settings

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kenneth.app.starlightlauncher.filesearchmodule.FileSearchModulePreferences
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainSettingsScreenViewModel(dataStore: DataStore<Preferences>) : ViewModel() {
    private val prefs = FileSearchModulePreferences.getInstance(dataStore)

    var includedSearchPaths by mutableStateOf<List<Uri>>(emptyList())
        private set

    init {
        viewModelScope.launch {
            prefs.includedPaths.collectLatest {
                includedSearchPaths = it.map { uri -> Uri.parse(uri) }
            }
        }
    }

    fun addSearchPath(uri: Uri) {
        viewModelScope.launch {
            prefs.includeNewPath(uri)
        }
    }

    fun removeSearchPath(uri: Uri) {
        viewModelScope.launch {
            prefs.removePath(uri)
        }
    }
}
