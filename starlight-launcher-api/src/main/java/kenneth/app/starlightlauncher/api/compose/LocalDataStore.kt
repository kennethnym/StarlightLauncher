package kenneth.app.starlightlauncher.api.compose

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

val LocalDataStore = staticCompositionLocalOf<DataStore<Preferences>> {
    error("No datastore instance provided")
}
