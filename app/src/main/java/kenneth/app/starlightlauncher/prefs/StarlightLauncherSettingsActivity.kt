package kenneth.app.starlightlauncher.prefs

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.preference.SettingsActivity
import kenneth.app.starlightlauncher.prefs.intents.PreferenceChangedIntent
import javax.inject.Inject

@AndroidEntryPoint
class StarlightLauncherSettingsActivity : SettingsActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject
    lateinit var extensionManager: ExtensionManager

    override fun createPreferenceFragment(): PreferenceFragmentCompat = RootSettingsFragment()

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d("hub", "pref changed")
        key?.let {
            sendBroadcast(PreferenceChangedIntent(key))
            Log.d("hub", "broadcast sent")
        }
    }
}