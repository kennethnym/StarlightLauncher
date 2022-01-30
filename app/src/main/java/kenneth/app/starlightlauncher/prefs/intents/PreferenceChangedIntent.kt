package kenneth.app.starlightlauncher.prefs.intents

import android.content.Intent

const val PREFERENCE_CHANGED_ACTION =
    "kenneth.app.hublauncher.prefs.SettingsActivity.PREFERENCE_CHANGED"

const val EXTRA_CHANGED_PREFERENCE_KEY = "prefKey"

/**
 * Creates an [Intent] with a preference changed action indicating that the value of the
 * included preference key is updated which can be accessed with [EXTRA_CHANGED_PREFERENCE_KEY].
 */
class PreferenceChangedIntent(prefKey: String) : Intent(PREFERENCE_CHANGED_ACTION) {
    init {
        putExtra(EXTRA_CHANGED_PREFERENCE_KEY, prefKey)
    }
}