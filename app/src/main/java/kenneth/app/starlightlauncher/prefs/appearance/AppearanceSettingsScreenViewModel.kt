package kenneth.app.starlightlauncher.prefs.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppearanceSettingsScreenViewModel @Inject constructor(
    private val appearancePreferenceManager: AppearancePreferenceManager
) : ViewModel() {
    var isBlurEffectEnabled by mutableStateOf(false)
        private set

    var isAppDrawerEnabled by mutableStateOf(DEFAULT_APP_DRAWER_ENABLED)
        private set

    init {
        with(viewModelScope) {
            launch {
                isBlurEffectEnabled = appearancePreferenceManager.isBlurEffectEnabled.first()
                appearancePreferenceManager.isBlurEffectEnabled
                    .collectLatest { isBlurEffectEnabled = it }
            }

            launch {
                appearancePreferenceManager.isAppDrawerEnabled.collectLatest {
                    isAppDrawerEnabled = it
                }
            }
        }
    }

    fun changeIsBlurEffectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appearancePreferenceManager.setBlurEffectEnabled(enabled)
        }
    }

    fun changeIsAppDrawerEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appearancePreferenceManager.setAppDrawerEnabled(enabled)
        }
    }
}
