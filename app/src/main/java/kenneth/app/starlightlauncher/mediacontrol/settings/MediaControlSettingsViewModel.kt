package kenneth.app.starlightlauncher.mediacontrol.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaControlSettingsViewModel @Inject constructor(
    private val mediaControlPreferenceManager: MediaControlPreferenceManager
) : ViewModel() {
    var isMediaControlEnabled by mutableStateOf(true)
        private set

    fun enableMediaControl() {
        viewModelScope.launch {
            mediaControlPreferenceManager.enableMediaControl()
        }
    }

    fun disableMediaControl() {
        viewModelScope.launch {
            mediaControlPreferenceManager.disableMediaControl()
        }
    }
}