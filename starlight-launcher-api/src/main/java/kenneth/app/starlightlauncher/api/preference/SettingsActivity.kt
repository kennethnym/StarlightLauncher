package kenneth.app.starlightlauncher.api.preference

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import kenneth.app.starlightlauncher.api.R

/**
 * An activity used by Starlight Launcher to show settings.
 * Your extension's settings should inherit this class
 * so that they follow Starlight's settings behavior and style
 */
abstract class SettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.settings_activity)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        findViewById<AppBarLayout>(R.id.app_bar_layout).setOnApplyWindowInsetsListener { v, insets ->
            v.updatePadding(
                top =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    insets.getInsets(WindowInsets.Type.systemBars())
                        .top
                else insets.systemWindowInsetTop
            )
            findViewById<FrameLayout>(R.id.settings_content).updatePadding(
                bottom = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    insets.getInsets(WindowInsets.Type.systemBars())
                        .bottom
                else insets.systemWindowInsetBottom
            )
            insets
        }

        toolbar = findViewById(R.id.settings_toolbar)
        setSupportActionBar(toolbar)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_content, createPreferenceFragment())
                .commit()
        }

//        PreferenceManager.getDefaultSharedPreferences(this)
//            .registerOnSharedPreferenceChangeListener(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        // Instantiate the new Fragment
        val args = pref.extras
        return pref.fragment?.let {
            val fragment = supportFragmentManager.fragmentFactory.instantiate(
                classLoader,
                it
            ).apply {
                arguments = args
            }

            // Replace the existing Fragment with the new Fragment
            supportFragmentManager.beginTransaction()
                .replace(R.id.settings_content, fragment)
                .addToBackStack(null)
                .commit()

            true
        } ?: false
    }

    abstract fun createPreferenceFragment(): PreferenceFragmentCompat
}