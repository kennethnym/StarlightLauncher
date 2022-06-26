package kenneth.app.starlightlauncher

import android.appwidget.AppWidgetHost
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.location.LocationManager
import android.media.session.MediaSessionManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.view.Choreographer
import android.view.inputmethod.InputMethodManager
import androidx.preference.PreferenceManager
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kenneth.app.starlightlauncher.api.LauncherEvent
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.api.util.EventChannel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import java.security.SecureRandom
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

internal const val DEFAULT_DISPATCHER = "DEFAULT_DISPATCHER"
internal const val MAIN_DISPATCHER = "MAIN_DISPATCHER"
internal const val IO_DISPATCHER = "IO_DISPATCHER"

@Module
@InstallIn(SingletonComponent::class)
internal object AppModule {
    @Provides
    @Singleton
    fun provideOkHttpClient() = OkHttpClient()

    @Provides
    @Singleton
    fun provideJsonHandler() = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    @Named(DEFAULT_DISPATCHER)
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    @Named(MAIN_DISPATCHER)
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Provides
    @Singleton
    @Named(IO_DISPATCHER)
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideSharedPreference(@ApplicationContext context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)

    @Provides
    @Singleton
    fun provideMediaSessionManager(@ApplicationContext context: Context) =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

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
    fun provideConnectivityManager(@ApplicationContext context: Context) =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @Provides
    @Singleton
    fun provideLocationManager(@ApplicationContext context: Context) =
        context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @Provides
    fun provideLocale() = Locale.getDefault()

    @Provides
    fun provideCalendar() = Calendar.getInstance()

    @Provides
    fun provideChoreographer() = Choreographer.getInstance()

    @Provides
    @Singleton
    fun provideBlurHandler(@ApplicationContext context: Context) = BlurHandler(context)

    @Provides
    @Singleton
    fun provideAppWidgetHost(@ApplicationContext context: Context) =
        AppWidgetHost(context.applicationContext, R.id.app_widget_host_id)

    @Provides
    @Singleton
    fun provideSecureRandom() = SecureRandom()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RandomProvider {
    @Binds
    abstract fun bindRandom(impl: SecureRandom): Random
}
