package kenneth.app.spotlightlauncher

import android.bluetooth.BluetoothManager
import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.net.wifi.WifiManager
import android.view.Choreographer
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.prefs.PinnedAppsPreferenceManager
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.prefs.appearance.IconPackManager
import kenneth.app.spotlightlauncher.prefs.datetime.DateTimePreferenceManager
import kenneth.app.spotlightlauncher.prefs.files.FilePreferenceManager
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.util.*
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOkHttpClient() = OkHttpClient()

    @Provides
    @Singleton
    fun provideJsonHandler() = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideSharedPreference(@ApplicationContext context: Context) =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @Singleton
    fun provideMediaSessionManager(@ApplicationContext context: Context) =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    @Provides
    @Singleton
    fun provideAppearancePreferenceManager(@ApplicationContext context: Context) =
        AppearancePreferenceManager.getInstance(context)

    @Provides
    @Singleton
    fun provideInputMethodManager(@ApplicationContext context: Context) =
        context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

    @Provides
    @Singleton
    fun provideBluetoothManager(@ApplicationContext context: Context) =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    @Provides
    @Singleton
    fun provideWifiManager(@ApplicationContext context: Context) =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @Provides
    @Singleton
    fun provideIconPackManager(@ApplicationContext context: Context) =
        IconPackManager.getInstance(context)

    @Provides
    @Singleton
    fun provideFilePreferenceManager(@ApplicationContext context: Context) =
        FilePreferenceManager.getInstance(context)

    @Provides
    @Singleton
    fun providePinnedAppsPreferenceManager(@ApplicationContext context: Context) =
        PinnedAppsPreferenceManager.getInstance(context)

    @Provides
    @Singleton
    fun provideDateTimePreferenceManager(@ApplicationContext context: Context) =
        DateTimePreferenceManager.getInstance(context)

    @Provides
    fun provideLocale() = Locale.getDefault()

    @Provides
    fun provideCalendar() = Calendar.getInstance()

    @Provides
    fun provideChoreographer() = Choreographer.getInstance()
}
