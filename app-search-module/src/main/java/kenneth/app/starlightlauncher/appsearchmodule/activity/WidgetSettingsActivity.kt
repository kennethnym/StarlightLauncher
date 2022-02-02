package kenneth.app.starlightlauncher.appsearchmodule.activity

import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import kenneth.app.starlightlauncher.appsearchmodule.R
import kenneth.app.starlightlauncher.appsearchmodule.fragment.SearchResultSettingsFragment
import kenneth.app.starlightlauncher.appsearchmodule.fragment.WidgetSettingsFragment

class WidgetSettingsActivity : AppCompatActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_content, WidgetSettingsFragment())
                .commit()
        }
    }

    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat?,
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
}