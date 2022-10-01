package kenneth.app.starlightlauncher.prefs

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

val PREF_KEY_ICON_PACK = stringPreferencesKey("appearance_icon_pack")

val PREF_KEY_TUTORIAL_FINISHED = booleanPreferencesKey("tutorial_finished")

val PREF_BLUR_EFFECT_ENABLED = booleanPreferencesKey("appearance_blur_effect_enabled")

val PREF_MEDIA_CONTROL_ENABLED = booleanPreferencesKey("media_control_enabled")
val PREF_NOTE_WIDGET_ENABLED = booleanPreferencesKey("note_widget_enabled")
val PREF_UNIT_CONVERTED_ENABLED = booleanPreferencesKey("unit_converted_enabled")

val PREF_SEARCH_MODULE_ORDER = stringPreferencesKey("pref_key_search_category_order")

val PREF_WIDGET_ORDER = stringPreferencesKey("pref_key_widget_order")
val PREF_ADDED_WIDGETS = stringPreferencesKey("pref_key_added_widgets")

val PREF_USE_24HR_CLOCK = booleanPreferencesKey("date_time_use_24hr_clock")
val PREF_SHOW_WEATHER = booleanPreferencesKey("date_time_show_weather")
val PREF_CLOCK_SIZE = stringPreferencesKey("date_time_clock_size")
val PREF_USE_AUTO_LOCATION = booleanPreferencesKey("date_time_use_auto_location")
val PREF_AUTO_LOCATION_CHECK_FREQUENCY =
    stringPreferencesKey("date_time_auto_location_check_frequency")
val PREF_WEATHER_LOCATION_LAT = floatPreferencesKey("date_time_weather_location_lat")
val PREF_WEATHER_LOCATION_LONG = floatPreferencesKey("date_time_weather_location_lat")
val PREF_WEATHER_LOCATION_NAME = stringPreferencesKey("date_time_weather_location_name")
val PREF_WEATHER_UNIT = stringPreferencesKey("date_time_weather_unit")

val PREF_NOTE_LIST = stringPreferencesKey("note_list")
