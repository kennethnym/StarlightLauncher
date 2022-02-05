package kenneth.app.starlightlauncher.prefs

import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.prefs.intents.PreferenceChangedIntent
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback,
    SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject
    lateinit var extensionManager: ExtensionManager

    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_activity)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        toolbar = findViewById<MaterialToolbar>(R.id.settings_toolbar).also {
            it.setOnApplyWindowInsetsListener { view, insets ->
                view.updatePadding(
                    top =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                        insets.getInsets(WindowInsets.Type.systemBars())
                            .top
                    else insets.systemWindowInsetTop
                )
                insets
            }
        }

        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_content, RootSettingsFragment())
                .commit()
        }

        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        val fragment = supportFragmentManager.fragmentFactory.instantiate(
            classLoader,
            pref.fragment
        ).apply {
            arguments = args
            setTargetFragment(caller, 0)
        }

        // Replace the existing Fragment with the new Fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.settings_content, fragment)
            .addToBackStack(null)
            .commit()

        return true
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        Log.d("hub", "pref changed")
        key?.let {
            sendBroadcast(PreferenceChangedIntent(key))
            Log.d("hub", "broadcast sent")
        }
    }
}