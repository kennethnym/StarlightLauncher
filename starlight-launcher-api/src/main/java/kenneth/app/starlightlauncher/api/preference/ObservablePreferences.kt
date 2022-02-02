package kenneth.app.starlightlauncher.api.preference

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.CallSuper
import androidx.preference.PreferenceManager
import java.util.*
import kotlin.reflect.KFunction2

typealias ObservablePreferencesListener<T> = (preferences: T, key: String) -> Unit

abstract class ObservablePreferences<T : ObservablePreferences<T>>(
    private val context: Context
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
    @CallSuper
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if (key != null) {
            setChanged()
            notifyObservers(key)
        }
    }

    fun addPreferencesListener(listener: ObservablePreferencesListener<T>) {
        addObserver { o, arg ->
            listener(o as T, arg as String)
        }
    }
}