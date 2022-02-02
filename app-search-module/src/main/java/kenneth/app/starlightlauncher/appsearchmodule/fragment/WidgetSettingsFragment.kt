package kenneth.app.starlightlauncher.appsearchmodule.fragment

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import kenneth.app.starlightlauncher.appsearchmodule.R

class WidgetSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.widget_preference, rootKey)
    }
}