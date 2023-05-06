package kenneth.app.starlightlauncher.mediacontrol.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.dataStore
import kenneth.app.starlightlauncher.prefs.PREF_MEDIA_CONTROL_ENABLED
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaControlPreferenceManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.dataStore

    val isMediaControlEnabled = context.dataStore.data
        .map {
            it[PREF_MEDIA_CONTROL_ENABLED]
                ?: context.resources.getBoolean(R.bool.default_media_control_enabled)
        }
        .distinctUntilChanged()

    suspend fun enableMediaControl() {
        dataStore.edit {
            it[PREF_MEDIA_CONTROL_ENABLED] = true
        }
    }

    suspend fun disableMediaControl() {
        dataStore.edit {
            it[PREF_MEDIA_CONTROL_ENABLED] = false
        }
    }
}
