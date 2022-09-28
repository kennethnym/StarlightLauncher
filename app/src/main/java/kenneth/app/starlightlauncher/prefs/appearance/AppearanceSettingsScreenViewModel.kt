package kenneth.app.starlightlauncher.prefs.appearance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppearanceSettingsScreenViewModel @Inject constructor(
    private val appearancePreferenceManager: AppearancePreferenceManager
) : ViewModel() {
    var isBlurEffectEnabled by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            appearancePreferenceManager.isBlurEffectEnabled
                .collectLatest { isBlurEffectEnabled = it }
        }
    }

    fun setIsBlurEffectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            appearancePreferenceManager.setBlurEffectEnabled(enabled)
        }
    }
}
