package kenneth.app.starlightlauncher.prefs

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.BuildConfig
import kenneth.app.starlightlauncher.HANDLED
import kenneth.app.starlightlauncher.MainActivity
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.intent.StarlightLauncherIntent
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.extension.ExtensionSettings
import javax.inject.Inject

@AndroidEntryPoint
internal class RootSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var extensionManager: ExtensionManager

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        loadExtensionSettings()

        findPreference<Preference>(getString(R.string.pref_key_launcher_version))
            ?.summary = BuildConfig.VERSION_NAME

        findPreference<Preference>(getString(R.string.pref_key_launcher_source_code))
            ?.setOnPreferenceClickListener {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.starlight_launcher_source_code_url))
                    )
                )
                false
            }

        findPreference<Preference>(getString(R.string.pref_key_launcher_provide_feedback))
            ?.setOnPreferenceClickListener {
                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(getString(R.string.starlight_launcher_feedback_url))
                    )
                )
                false
            }

        findPreference<Preference>(getString(R.string.pref_key_launcher_author))
            ?.setOnPreferenceClickListener {
                startActivity(
                    Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(
                            Intent.EXTRA_EMAIL,
                            getString(R.string.starlight_launcher_author_email)
                        )
                    }
                )
                false
            }

        findPreference<Preference>(getString(R.string.pref_key_restart_launcher))
            ?.setOnPreferenceClickListener {
                restartLauncher()
                false
            }
    }

    override fun onResume() {
        super.onResume()
        changeToolbarTitle()
    }

    private fun loadExtensionSettings() {
        findPreference<PreferenceCategory>(getString(R.string.pref_category_search_behavior))?.let { category ->
            // get all search module settings
            extensionManager
                .getIntentsForSettingsCategory(StarlightLauncherIntent.CATEGORY_SEARCH_MODULE_SETTINGS)
                .forEach { extSettings ->
                    createPreferenceForExtensionSettings(extSettings)
                        ?.let { category.addPreference(it) }
                }
        }

        findPreference<PreferenceCategory>(getString(R.string.pref_category_widgets))?.let { category ->
            // get all widget settings
            extensionManager
                .getIntentsForSettingsCategory(StarlightLauncherIntent.CATEGORY_WIDGET_SETTINGS)
                .forEach { extSettings ->
                    createPreferenceForExtensionSettings(extSettings)
                        ?.let { category.addPreference(it) }
                }
        }
    }

    private fun createPreferenceForExtensionSettings(settings: ExtensionSettings) =
        context?.let {
            Preference(it).apply {
                title = settings.title
                summary = settings.description
                icon = settings.icon?.apply {
                    setTint(TypedValue().run {
                        context.theme.resolveAttribute(R.attr.colorPrimary, this, true)
                        data
                    })
                }
                setOnPreferenceClickListener {
                    startActivity(settings.intent)
                    HANDLED
                }
            }
        }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.title_activity_settings)
    }

    private fun restartLauncher() {
        context?.let { context ->
            Intent(context, MainActivity::class.java).run {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(this)
                activity?.finish()
            }
        }
    }
}