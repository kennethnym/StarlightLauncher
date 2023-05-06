package kenneth.app.starlightlauncher.appsearchmodule.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModulePreferences
import kenneth.app.starlightlauncher.appsearchmodule.DEFAULT_SHOW_APP_NAMES
import kenneth.app.starlightlauncher.appsearchmodule.DEFAULT_SHOW_PINNED_APP_NAMES
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainSettingsScreenViewModel(dataStore: DataStore<Preferences>) : ViewModel() {
    private val prefs = AppSearchModulePreferences.getInstance(dataStore)

    var shouldShowAppNames by mutableStateOf(DEFAULT_SHOW_APP_NAMES)
        private set

    var shouldShowPinnedAppNames by mutableStateOf(DEFAULT_SHOW_PINNED_APP_NAMES)
        private set

    init {
        with(viewModelScope) {
            launch {
                prefs.shouldShowAppNames.collectLatest {
                    shouldShowAppNames = it
                }
            }

            launch {
                prefs.shouldShowPinnedAppNames.collectLatest {
                    shouldShowPinnedAppNames = it
                }
            }
        }
    }

    fun setAppNamesVisibility(isVisible: Boolean) {
        viewModelScope.launch {
            prefs.setAppNamesVisibility(isVisible)
        }
    }

    fun setPinnedAppNamesVisibility(isVisible: Boolean) {
        viewModelScope.launch {
            prefs.setPinnedAppNamesVisibility(isVisible)
        }
    }
}