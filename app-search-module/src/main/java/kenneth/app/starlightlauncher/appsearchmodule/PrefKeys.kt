package kenneth.app.starlightlauncher.appsearchmodule

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val PREF_KEY_SHOW_APP_NAMES = booleanPreferencesKey("pref_key_show_app_names")
val PREF_KEY_SHOW_PINNED_APP_NAMES = booleanPreferencesKey("pref_key_show_pinned_app_names")
val PREF_KEY_PINNED_APPS = stringPreferencesKey("pref_key_pinned_apps")
