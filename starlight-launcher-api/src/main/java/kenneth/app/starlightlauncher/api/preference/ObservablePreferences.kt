package kenneth.app.starlightlauncher.api.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java.util.*

typealias ObservablePreferencesListener = (event: PreferencesChanged) -> Unit

data class PreferencesChanged(
    val status: Status,
    val key: String,
) {
    enum class Status {
        KEY_ADDED_OR_CHANGED,
        KEY_REMOVED,
    }
}

abstract class ObservablePreferences<T : ObservablePreferences<T>>(
    context: Context
) :
    SharedPreferences.OnSharedPreferenceChangeListener,
    Observable() {
    /**
     * The instance of [SharedPreferences] that this class is operating on.
     */
    val sharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
            .also { it.registerOnSharedPreferenceChangeListener(this) }

    /**
     * Overriding class overriding this method should call super *after* the overriding code.
     */
    final override fun onSharedPreferenceChanged(
        sharedPreferences: SharedPreferences?,
        key: String?
    ) {
        if (key != null && sharedPreferences != null) {
            updateValue(sharedPreferences, key)
            setChanged()
            notifyObservers(
                PreferencesChanged(
                    status = when {
                        sharedPreferences.contains(key) -> PreferencesChanged.Status.KEY_ADDED_OR_CHANGED
                        else -> PreferencesChanged.Status.KEY_REMOVED
                    },
                    key,
                )
            )
        }
    }

    fun addOnPreferenceChangedListener(listener: ObservablePreferencesListener) {
        addObserver { o, arg ->
            if (arg is PreferencesChanged && arg.status == PreferencesChanged.Status.KEY_ADDED_OR_CHANGED) {
                listener(arg)
            }
        }
    }

    fun addOnPreferenceRemovedListener(listener: ObservablePreferencesListener) {
        addObserver { o, arg ->
            if (arg is PreferencesChanged && arg.status == PreferencesChanged.Status.KEY_REMOVED) {
                listener(arg)
            }
        }
    }

    protected abstract fun updateValue(sharedPreferences: SharedPreferences, key: String)
}