package kenneth.app.starlightlauncher.appsearchmodule.activity

import android.os.Build
import android.os.Bundle
import android.view.WindowInsets
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import kenneth.app.starlightlauncher.api.preference.SettingsActivity
import kenneth.app.starlightlauncher.appsearchmodule.R
import kenneth.app.starlightlauncher.appsearchmodule.fragment.SearchResultSettingsFragment

class SearchModuleSettingsActivity : SettingsActivity(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    override fun createPreferenceFragment() = SearchResultSettingsFragment()
}