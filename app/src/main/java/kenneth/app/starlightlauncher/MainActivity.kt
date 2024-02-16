package kenneth.app.starlightlauncher

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.appwidget.AppWidgetHost
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.drawable.TransitionDrawable
import android.os.Build
import android.os.Bundle
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.DeprecatedSinceApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.api.view.OptionMenuBuilder
import kenneth.app.starlightlauncher.databinding.ActivityMainBinding
import kenneth.app.starlightlauncher.extension.ExtensionManager
import kenneth.app.starlightlauncher.home.HomeScreenViewPagerAdapter
import kenneth.app.starlightlauncher.home.POSITION_HOME_SCREEN_VIEW_PAGER_APP_DRAWER
import kenneth.app.starlightlauncher.home.POSITION_HOME_SCREEN_VIEW_PAGER_HOME
import kenneth.app.starlightlauncher.prefs.PREF_KEY_TUTORIAL_FINISHED
import kenneth.app.starlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.starlightlauncher.prefs.appearance.InstalledIconPack
import kenneth.app.starlightlauncher.searching.Searcher
import kenneth.app.starlightlauncher.setup.PREF_SETUP_FINISHED
import kenneth.app.starlightlauncher.setup.SetupActivity
import kenneth.app.starlightlauncher.util.calculateDominantColor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Named

private const val BACKGROUND_TRANSITION_DURATION_MS = 200

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
    lateinit var launcherState: LauncherState

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
    lateinit var launcherFragmentFactory: LauncherFragmentFactory

    @Inject
    lateinit var launcherEventChannel: LauncherEventChannel

    private lateinit var binding: ActivityMainBinding

    private lateinit var currentConfig: Configuration

    private var currentWallpaper: Bitmap? = null

    private var isDarkModeActive = false

    private var isSetupTriggered = false

    /**
     * The current back pressed callback that will be called
     * when overlay is visible and the back button is pressed.
     * This is only set when overlay is visible.
     */
    private var overlayBackPressedCallback: OverlayOnBackPressedCallback? = null

    private val onPageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            binding.homeScreenViewPager.background.run {
                if (this !is TransitionDrawable) return

                if (position == POSITION_HOME_SCREEN_VIEW_PAGER_APP_DRAWER) {
                    startTransition(BACKGROUND_TRANSITION_DURATION_MS)
                } else {
                    reverseTransition(BACKGROUND_TRANSITION_DURATION_MS)
                }
            }
        }

        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int
        ) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels)
            if (position == POSITION_HOME_SCREEN_VIEW_PAGER_APP_DRAWER) {
                onBackPressedDispatcher.addCallback(
                    AllAppsScreenBackPressedCallback(true)
                )
            }
        }
    }

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

        currentConfig = Configuration(resources.configuration)

        val setupFinished = runBlocking {
            dataStore.data.first()[PREF_SETUP_FINISHED] ?: false
        }
        if (!setupFinished) {
            isSetupTriggered = true
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            getCurrentWallpaper()
        }
        updateAdaptiveColors()

        extensionManager.run {
            launcherApi = this@MainActivity.launcherApi
            loadExtensions()
        }

        // enable edge-to-edge app experience
        WindowCompat.setDecorFitsSystemWindows(window, false)

        launcherState.apply {
            screenWidth = resources.displayMetrics.widthPixels
            screenHeight = resources.displayMetrics.heightPixels
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        binding.homeScreenViewPager.apply {
            offscreenPageLimit = 1
            isUserInputEnabled = false
            adapter = HomeScreenViewPagerAdapter(this@MainActivity, this)

            background.let {
                if (it is TransitionDrawable) it.reverseTransition(0)
            }
        }.also {
            it.registerOnPageChangeCallback(onPageChangeCallback)
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

        onBackPressedDispatcher.addCallback { }
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            checkWallpaper()
        }
    }

    override fun onDestroy() {
        if (!isSetupTriggered) {
            cleanup()
        }
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        val isNightModeChanged = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) {
            currentConfig.isNightModeActive != newConfig.isNightModeActive
        } else {
            val wasNightModeActive =
                currentConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val isNightModeActive =
                newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            newConfig.diff(currentConfig) != 0 && wasNightModeActive != isNightModeActive
        }

        currentConfig = Configuration(newConfig)

        if (isNightModeChanged) {
            onNightModeChanged()
        }
    }

    fun showOverlay(fragment: Fragment) {
        binding.overlay.show()

        val currentFragment =
            supportFragmentManager.findFragmentById(binding.overlay.contentContainerId)
        supportFragmentManager.commit {
            if (currentFragment != null) remove(currentFragment)
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

    fun showOptionMenu(builder: OptionMenuBuilder) {
        binding.optionMenu.show(builder)
    }

    fun closeOptionMenu() {
        binding.optionMenu.hide()
    }

    private fun cleanup() {
        extensionManager.cleanUpExtensions()
        appWidgetHost.stopListening()
    }

    private fun onNightModeChanged() {
        lifecycleScope.launch {
            launcherEventChannel.add(
                NightModeChanged(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2)
                        currentConfig.isNightModeActive
                    else
                        currentConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
                )
            )
        }
    }

    private fun attachListeners() {
        with(binding.root) {
            setOnApplyWindowInsetsListener { _, insets ->
                launcherState.statusBarHeight = WindowInsetsCompat.toWindowInsetsCompat(insets)
                    .getInsets(WindowInsetsCompat.Type.systemBars()).top

                binding.statusBarShade.layoutParams = binding.statusBarShade.layoutParams.apply {
                    height = launcherState.statusBarHeight * 2
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
            launcherState.apply {
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
     *
     * Due to this ongoing issue: https://issuetracker.google.com/issues/237124750?pli=1,
     * it is next to impossible to obtain the current wallpaper above API32,
     * because MANAGE_EXTERNAL_STORAGE is required just to do so,
     * and usually the Play Store rejects apps that require such permission.
     */
    @SuppressLint("MissingPermission")
    @DeprecatedSinceApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkWallpaper() {
        val currentWallpaper = this.currentWallpaper ?: return
        lifecycleScope.launch {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                val isWallpaperChanged = withContext(ioDispatcher) {
                    WallpaperManager.getInstance(this@MainActivity).fastDrawable
                        ?.toBitmap()?.sameAs(currentWallpaper)
                        ?: false
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
    @SuppressLint("MissingPermission")
    @DeprecatedSinceApi(Build.VERSION_CODES.TIRAMISU)
    private fun getCurrentWallpaper() {
        if (
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        ) {
            currentWallpaper = WallpaperManager.getInstance(this).fastDrawable?.toBitmap()
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

    private inner class AllAppsScreenBackPressedCallback(enabled: Boolean) :
        OnBackPressedCallback(enabled) {
        override fun handleOnBackPressed() {
            binding.homeScreenViewPager.currentItem = POSITION_HOME_SCREEN_VIEW_PAGER_HOME
            remove()
        }
    }
}
