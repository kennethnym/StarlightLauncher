package kenneth.app.starlightlauncher

import android.Manifest
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.databinding.ActivityMainBinding
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.home.HomeScreenViewPagerAdapter
import kenneth.app.starlightlauncher.prefs.PREF_KEY_TUTORIAL_FINISHED
import kenneth.app.starlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.starlightlauncher.prefs.appearance.InstalledIconPack
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.util.calculateDominantColor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named

@Module
@InstallIn(ActivityComponent::class)
internal object MainActivityModule {
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

/**
 * The home screen of the launcher.
 */
@AndroidEntryPoint
internal class MainActivity : AppCompatActivity(), ViewTreeObserver.OnGlobalLayoutListener {
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
    lateinit var launcherApi: StarlightLauncherApi

    @Inject
    @Named(MAIN_DISPATCHER)
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    @Named(IO_DISPATCHER)
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var appWidgetHost: AppWidgetHost

    @Inject
    lateinit var tutorialOverlay: TutorialOverlay

    @Inject
    lateinit var bindingRegister: BindingRegister

    @Inject
    lateinit var launcherFragmentFactory: LauncherFragmentFactory

    private lateinit var binding: ActivityMainBinding

    private var currentWallpaper: Bitmap? = null

    private var isDarkModeActive = false

    /**
     * The current back pressed callback that will be called
     * when overlay is visible and the back button is pressed.
     * This is only set when overlay is visible.
     */
    private var overlayBackPressedCallback: OverlayOnBackPressedCallback? = null

    override fun onStart() {
        super.onStart()
        appWidgetHost.startListening()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        supportFragmentManager.fragmentFactory = EntryPointAccessors
            .fromActivity(
                this,
                LauncherFragmentFactoryEntryPoint::class.java
            )
            .launcherFragmentFactory()

        super.onCreate(savedInstanceState)

        getCurrentWallpaper()
        updateAdaptiveColors()

        extensionManager.run {
            launcherApi = this@MainActivity.launcherApi
            loadExtensions()
        }

        // enable edge-to-edge app experience
        WindowCompat.setDecorFitsSystemWindows(window, false)

        appState.apply {
            screenWidth = resources.displayMetrics.widthPixels
            screenHeight = resources.displayMetrics.heightPixels
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.homeScreenViewPager.apply {
            isUserInputEnabled = false
            adapter = HomeScreenViewPagerAdapter(this@MainActivity)
        }

        isDarkModeActive =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                resources.configuration.isNightModeActive
            else
                resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        setContentView(binding.root)
        setWallpaper()
        runBlocking {
            appearancePreferenceManager.iconPack.first().let {
                if (it is InstalledIconPack) it.load()
            }
        }
        attachListeners()

        // handle blur effect preference
        lifecycleScope.launch {
            appearancePreferenceManager.isBlurEffectEnabled.collectLatest {
                blurHandler.let { blurHandler ->
                    if (blurHandler is BlurHandlerImpl)
                        blurHandler.isBlurEffectEnabled = it
                }
            }
        }
    }

    override fun onGlobalLayout() {
        lifecycleScope.launch {
            val isTutorialFinished = dataStore.data.first()[PREF_KEY_TUTORIAL_FINISHED] ?: false
            // only show spotlight after layout is complete
            // because spotlight relies on the global position of views
            // in order to position itself correctly to point to views
            if (!isTutorialFinished) showSpotlight()

            binding.root.viewTreeObserver.removeOnGlobalLayoutListener(this@MainActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        checkWallpaper()
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    fun showOverlay(fragment: Fragment) {
        binding.overlay.show()
        supportFragmentManager.commit {
            add(binding.overlay.contentContainerId, fragment)
        }
        onBackPressedDispatcher.addCallback(
            OverlayOnBackPressedCallback(true).also {
                overlayBackPressedCallback = it
            }
        )
    }

    fun closeOverlay() {
        overlayBackPressedCallback?.remove()
        overlayBackPressedCallback = null
        binding.overlay.close()
    }

    private fun cleanup() {
        extensionManager.cleanUpExtensions()
        appWidgetHost.stopListening()
    }

    private fun attachListeners() {
        with(binding.root) {
            setOnApplyWindowInsetsListener { _, insets ->
                appState.statusBarHeight = WindowInsetsCompat.toWindowInsetsCompat(insets)
                    .getInsets(WindowInsetsCompat.Type.systemBars()).top

                binding.statusBarShade.layoutParams = binding.statusBarShade.layoutParams.apply {
                    height = appState.statusBarHeight * 2
                }

                insets
            }

            viewTreeObserver.addOnGlobalLayoutListener(this@MainActivity)
        }
    }

    /**
     * Shows a tutorial overlay for the user to guide them through how to use the launcher.
     */
    private fun showSpotlight() {
        tutorialOverlay.start()
    }

    /**
     * Update the current adaptive color scheme by finding the dominant color of the current wallpaper.
     * If the launcher doesn't have permission to access the current wallpaper,
     * the launcher will fallback to dark theme.
     * TODO: maybe make the fallback theme configurable by the user in settings
     */
    private fun updateAdaptiveColors() {
        val currentWallpaper = this.currentWallpaper
        if (
            currentWallpaper != null &&
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            appState.apply {
                val adaptiveBackgroundColor = currentWallpaper.calculateDominantColor()
                val white = getColor(android.R.color.white)

                setTheme(
                    if (ColorUtils.calculateContrast(white, adaptiveBackgroundColor) > 1.5f)
                        R.style.DarkLauncherTheme
                    else
                        R.style.LightLauncherTheme
                )
            }
        } else {
            setTheme(R.style.DarkLauncherTheme)
        }
    }

    /**
     * Checks if the wallpaper is updated. If it is, recreate activity to update adaptive theme.
     */
    private fun checkWallpaper() {
        val currentWallpaper = this.currentWallpaper ?: return
        CoroutineScope(mainDispatcher).launch {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                val isWallpaperChanged = withContext(ioDispatcher) {
                    val wallpaper =
                        WallpaperManager.getInstance(this@MainActivity).fastDrawable.toBitmap()
                    !wallpaper.sameAs(currentWallpaper)
                }
                if (isWallpaperChanged) {
                    recreate()
                }
            }
        }
    }

    /**
     * Fetches the current wallpaper if the launcher has permission to do so.
     */
    private fun getCurrentWallpaper() {
        if (
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            currentWallpaper = WallpaperManager.getInstance(this).fastDrawable.toBitmap()
        }
    }

    private fun setWallpaper() {
        currentWallpaper?.let { wallpaper ->
            binding.wallpaperImage.setImageBitmap(wallpaper)
            blurHandler.let {
                if (it is BlurHandlerImpl) it.changeWallpaper(wallpaper)
            }
        }
    }

    private fun handleGestureNav(bundle: Bundle) {

    }

    /**
     * Called when overlay is visible and the back button is pressed.
     */
    private inner class OverlayOnBackPressedCallback(enabled: Boolean) :
        OnBackPressedCallback(enabled) {
        override fun handleOnBackPressed() {
            binding.overlay.close()
        }
    }
}
