package kenneth.app.spotlightlauncher

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.api.SpotlightLauncherApi
import kenneth.app.spotlightlauncher.api.utils.BlurHandler
import kenneth.app.spotlightlauncher.databinding.ActivityMainBinding
import kenneth.app.spotlightlauncher.extension.ExtensionManager
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.prefs.appearance.InstalledIconPack
import kenneth.app.spotlightlauncher.searching.Searcher
import kenneth.app.spotlightlauncher.utils.*
import javax.inject.Inject

/**
 * Called when back button is pressed.
 * If handler returns true, subsequent handlers will not be called.
 */
typealias BackPressHandler = () -> Boolean

@Module
@InstallIn(ActivityComponent::class)
object MainActivityModule {
    @Provides
    fun provideMainActivity(@ActivityContext context: Context): MainActivity? {
        var ctx = context
        while (ctx is ContextWrapper) {
            if (ctx is MainActivity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }
}

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var searcher: Searcher

    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var blurHandler: BlurHandler

    @Inject
    lateinit var appState: AppState

    @Inject
    lateinit var appearancePreferenceManager: AppearancePreferenceManager

    @Inject
    lateinit var inputMethodManager: InputMethodManager

    @Inject
    lateinit var permissionHandler: PermissionHandler

    @Inject
    lateinit var launcherApi: SpotlightLauncherApi

    private lateinit var binding: ActivityMainBinding

    private var backPressedCallbacks = mutableListOf<BackPressHandler>()

    private var isDarkModeActive = false

    fun addBackPressListener(handler: BackPressHandler) {
        backPressedCallbacks.add(handler)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateAdaptiveColors()
        setTheme(appState.themeStyleId)

        launcherApi.let {
            if (it is SpotlightLauncherApiImpl) it.context = this
        }

        extensionManager.loadExtensions()

        // enable edge-to-edge app experience
        WindowCompat.setDecorFitsSystemWindows(window, false)

        appState.apply {
            screenWidth = resources.displayMetrics.widthPixels
            screenHeight = resources.displayMetrics.heightPixels
        }

        binding = ActivityMainBinding.inflate(layoutInflater).also {
            BindingRegister.activityMainBinding = it
        }
        isDarkModeActive =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                resources.configuration.isNightModeActive
            else
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        setContentView(binding.root)
        appearancePreferenceManager.iconPack.let {
            if (it is InstalledIconPack) it.load()
        }
        permissionHandler.handlePermissionRequestsForActivity(this)
        attachListeners()
        askForReadExternalStoragePermission()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        isDarkModeActive =
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) newConfig.isNightModeActive
            else newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        if (binding.widgetsPanel.isExpanded) {
            if (isDarkModeActive) {
                disableLightStatusBar()
            } else {
                enableLightStatusBar()
            }
        }
    }

    override fun onBackPressed() {
        val isHandled = backPressedCallbacks.fold(false) { _, cb -> cb() }
        Log.d("hub", "is handled? $isHandled")
        if (!isHandled) {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        updateWallpaper()
    }

    override fun onStop() {
        cleanup()
        super.onStop()
    }

    override fun onPause() {
        cleanup()
        super.onPause()
    }

    override fun onContextItemSelected(item: MenuItem): Boolean =
        appState.contextMenuCallbackForView?.onContextItemSelected(item)
            ?: super.onContextItemSelected(item)

    override fun onContextMenuClosed(menu: Menu) {
        appState.contextMenuCallbackForView?.onContextMenuClosed()
            ?: super.onContextMenuClosed(menu)
    }

    private fun cleanup() {
//        searchResultAdapter.cleanup()
    }

    /**
     * a temporary function to ask for READ_EXTERNAL_STORAGE permission
     */
    private fun askForReadExternalStoragePermission() {
        // TODO: a temporary function to ask for READ_EXTERNAL_STORAGE permission
        // TODO: in prod this should be done during setup
        permissionHandler.run {
            requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun attachListeners() {
        with(binding.root) {
            setOnApplyWindowInsetsListener { _, insets ->
                appState.statusBarHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val systemBarsInset = insets.getInsets(WindowInsets.Type.systemBars())
                    systemBarsInset.top
                } else {
                    insets.systemWindowInsetTop
                }

                insets
            }

            viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        updateWallpaper()
                    }
                }
            )
        }
    }

    /**
     * Update the current adaptive color scheme by finding the dominant color of the current wallpaper.
     */
    private fun updateAdaptiveColors() {
        if (
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            val wallpaperBitmap = WallpaperManager.getInstance(this).fastDrawable.toBitmap()
            appState.apply {
                val adaptiveBackgroundColor = wallpaperBitmap.calculateDominantColor()
                val white = getColor(android.R.color.white)
                val black = ColorUtils.setAlphaComponent(getColor(android.R.color.black), 0x80)

                setTheme(
                    if (ColorUtils.calculateContrast(white, adaptiveBackgroundColor) > 1.5f)
                        R.style.DarkLauncherTheme
                    else
                        R.style.LightLauncherTheme
                )
            }

            if (appState.isInitialStart) {
                appState.isInitialStart = false
            } else {
                // restart activity after changing theme
                restartActivity()
            }
        }
    }

    /**
     * Restarts the current activity.
     * Credit to [this gist](https://gist.github.com/alphamu/f2469c28e17b24114fe5)
     */
    private fun restartActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    @SuppressLint("InlinedApi")
    private fun enableLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window
                .insetsController
                ?.setSystemBarsAppearance(
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
        } else if (Build.VERSION.SDK_INT in (Build.VERSION_CODES.M..Build.VERSION_CODES.Q) && !isDarkModeActive) {
            binding.root.systemUiVisibility =
                binding.root.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }

    private fun disableLightStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window
                .insetsController
                ?.setSystemBarsAppearance(
                    0,
                    WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                )
        } else
            binding.root.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    }

    /**
     * Gets the current wallpaper and renders it to binding.wallpaperImage
     */
    private fun updateWallpaper() {
        if (
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            val wallpaper = WallpaperManager.getInstance(this).fastDrawable
            binding.wallpaperImage.setImageDrawable(wallpaper)

            if (binding.wallpaperImage.width > 0 && binding.wallpaperImage.height > 0) {
                blurHandler.changeWallpaper(viewToBitmap(binding.wallpaperImage))
            }
        }
    }
}
