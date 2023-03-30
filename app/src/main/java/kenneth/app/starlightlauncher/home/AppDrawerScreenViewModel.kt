package kenneth.app.starlightlauncher.home

import android.content.pm.LauncherActivityInfo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kenneth.app.starlightlauncher.api.IconPack
import kenneth.app.starlightlauncher.api.LauncherEvent
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class AppDrawerScreenViewModel @Inject constructor() : ViewModel() {
    private val _appList = MutableLiveData<List<LauncherActivityInfo>>()
    val appList: LiveData<List<LauncherActivityInfo>> = _appList

    private val _iconPack = MutableLiveData<IconPack>()
    val iconPack: LiveData<IconPack> = _iconPack

    fun setLauncherInstance(launcher: StarlightLauncherApi) {
        _appList.value = launcher.installedApps

        with(viewModelScope) {
            launch {
                launcher.addLauncherEventListener {
                    when (it) {
                        is LauncherEvent.NewAppsInstalled,
                        is LauncherEvent.AppRemoved -> {
                            _appList.postValue(launcher.installedApps)
                        }
                    }
                }
            }

            launch {
                launcher.iconPack
                    .distinctUntilChanged()
                    .collectLatest {
                        _iconPack.postValue(it)
                    }
            }
        }
    }
}