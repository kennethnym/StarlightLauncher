package kenneth.app.starlightlauncher.prefs.appearance

import android.graphics.Bitmap
import android.os.Bundle
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.IO_DISPATCHER
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.utils.toPx
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Named

/**
 * A settings fragment that lets user change the icon pack.
 */
@AndroidEntryPoint
internal class IconPackSettingsFragment : PreferenceFragmentCompat() {
    @Inject
    lateinit var appearancePreferenceManager: AppearancePreferenceManager

    @Inject
    lateinit var iconPackManager: IconPackManager

    @Inject
    @Named(IO_DISPATCHER)
    lateinit var ioDispatcher: CoroutineDispatcher

    private val noCurrentIconPackPreference by lazy {
        context?.let {
            Preference(it).apply {
                isSelectable = false
                isPersistent = false
                title = getString(R.string.appearance_no_current_icon_pack)
            }
        }
    }

    private val noSupportedIconPacksPreference by lazy {
        context?.let {
            Preference(it).apply {
                isSelectable = false
                isPersistent = false
                title = getString(R.string.appearance_no_supported_icon_packs)
            }
        }
    }

    private var loadingSnackBar: Snackbar? = null

    private var installedIconPacksCategory: PreferenceCategory? = null
    private var currentIconPackCategory: PreferenceCategory? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.icon_pack_preferences, rootKey)

        currentIconPackCategory =
            findPreference(getString(R.string.appearance_current_icon_pack_category))

        installedIconPacksCategory =
            findPreference(getString(R.string.appearance_installed_icon_pack_category))

        findPreference<Preference>(getString(R.string.appearance_revert_icon_pack))
            ?.setOnPreferenceClickListener {
                restoreDefaultIcons()
                true
            }

        showIconPacks()
        changeToolbarTitle()
    }

    /**
     * Creates a Preference entry for the given icon pack.
     */
    private fun iconPackPreference(iconPack: InstalledIconPack) =
        context?.let {
            Preference(it).apply {
                key = iconPack.packageName
                title = iconPack.name
                icon = Bitmap.createScaledBitmap(iconPack.icon, 48.toPx(), 48.toPx(), false)
                    .toDrawable(resources)
            }
        }

    private fun changeToolbarTitle() {
        activity?.findViewById<MaterialToolbar>(R.id.settings_toolbar)?.title =
            getString(R.string.appearance_icon_pack_title)
    }

    private fun changeIconPack(newIconPack: InstalledIconPack) {
        lifecycleScope.launch {
            withContext(ioDispatcher) {
                appearancePreferenceManager.changeIconPack(newIconPack)
                newIconPack.load()
            }

            hideLoadingSnackbar()
            showIconPacks()
            enableIconPackPrefs()
        }

        disableIconPackPrefs()
        showLoadingSnackbar()
    }

    private fun restoreDefaultIcons() {
        val selectedIconPack = appearancePreferenceManager.iconPack
        if (selectedIconPack is DefaultIconPack) {
            Snackbar.make(
                listView,
                getString(R.string.appearance_icon_pack_already_using_default),
                Snackbar.LENGTH_LONG
            ).also { it.show() }
        } else {
            appearancePreferenceManager.useDefaultIconPack()
            showIconPacks()
        }
    }

    /**
     * Displays the currently active icon pack and installed icon packs
     */
    private fun showIconPacks() {
        currentIconPackCategory?.let { category ->
            category.removeAll()
            appearancePreferenceManager.iconPack
                .let { selectedIconPack ->
                    if (selectedIconPack is InstalledIconPack)
                        iconPackPreference(selectedIconPack)?.apply {
                            isSelectable = false
                            isPersistent = false
                        }
                    else noCurrentIconPackPreference
                }
                ?.let { category.addPreference(it) }
        }

        installedIconPacksCategory?.let { category ->
            category.removeAll()

            val installedIconPacks = iconPackManager.queryInstalledIconPacks()
            val selectedIconPack = appearancePreferenceManager.iconPack

            if (installedIconPacks.isNotEmpty()) {
                installedIconPacks.forEach { iconPackEntry ->
                    if (selectedIconPack is InstalledIconPack &&
                        iconPackEntry.key != selectedIconPack.packageName
                        || selectedIconPack is DefaultIconPack
                    ) {
                        iconPackPreference(iconPackEntry.value)?.also { pref ->
                            pref.setOnPreferenceClickListener {
                                changeIconPack(iconPackEntry.value)
                                true
                            }
                        }?.let { category.addPreference(it) }
                    }
                }
            } else {
                noSupportedIconPacksPreference?.let {
                    category.addPreference(it)
                }
            }

            return
        }
    }

    private fun disableIconPackPrefs() {
        installedIconPacksCategory?.let {
            for (i in 0 until it.preferenceCount) {
                it.getPreference(i).apply {
                    isSelectable = false
                    isPersistent = false
                }
            }
        }
    }

    private fun enableIconPackPrefs() {
        installedIconPacksCategory?.let {
            for (i in 0 until it.preferenceCount) {
                it.getPreference(i).apply {
                    isSelectable = true
                    isPersistent = true
                }
            }
        }
    }

    private fun showLoadingSnackbar() {
        loadingSnackBar = Snackbar.make(
            listView,
            getString(R.string.appearance_applying_icon_pack),
            Snackbar.LENGTH_INDEFINITE,
        ).also { it.show() }
    }

    private fun hideLoadingSnackbar() {
        loadingSnackBar?.dismiss()
    }
}