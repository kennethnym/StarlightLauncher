package kenneth.app.starlightlauncher.datetime

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kenneth.app.starlightlauncher.api.NominatimApi
import kenneth.app.starlightlauncher.api.Place
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherLocationPickerViewModel @Inject constructor(
    private val nominatimApi: NominatimApi
) : ViewModel() {
    var locations by mutableStateOf<List<Place>?>(null)
        private set

    var isSearching by mutableStateOf(false)
        private set

    var error by mutableStateOf<Throwable?>(null)
        private set

    fun searchForLocations(searchTerm: String) {
        isSearching = true
        viewModelScope.launch {
            val result = nominatimApi.searchForLocations(searchTerm)
            Log.d("starlight", "result $result")
            when {
                result.isSuccess -> locations = result.getOrNull()
                result.isFailure -> error = result.exceptionOrNull()
            }
            isSearching = false
        }
    }

    fun clearSearchResults() {
        locations = null
    }
}