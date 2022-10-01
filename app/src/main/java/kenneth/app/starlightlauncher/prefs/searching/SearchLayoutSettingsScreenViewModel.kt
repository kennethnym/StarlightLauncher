package kenneth.app.starlightlauncher.prefs.searching

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.util.swap
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class SearchLayoutSettingsScreenViewModel @Inject constructor(
    private val extensionManager: ExtensionManager,
    private val searchPreferenceManager: SearchPreferenceManager
) : ViewModel() {
    var searchModuleOrder by mutableStateOf(emptyList<SearchModule.Metadata>())
        private set

    init {
        viewModelScope.launch {
            searchPreferenceManager.searchModuleOrder
                .map { order ->
                    order.mapNotNull {
                        extensionManager.lookupSearchModule(it)?.metadata
                    }
                }
                .collectLatest {
                    searchModuleOrder = it
                }
        }
    }

    fun changeSearchModuleOrder(fromPosition: Int, toPosition: Int) {
        searchModuleOrder = searchModuleOrder.toMutableList().apply {
            swap(fromPosition, toPosition)
        }
        viewModelScope.launch {
            searchPreferenceManager.changeSearchCategoryOrder(fromPosition, toPosition)
        }
    }
}