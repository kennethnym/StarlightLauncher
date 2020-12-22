package kenneth.app.spotlightlauncher

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.view.Choreographer
import androidx.preference.Preference
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.prefs.PinnedAppsPreferenceManager
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.prefs.appearance.IconPackManager
import kenneth.app.spotlightlauncher.prefs.files.FilePreferenceManager
import okhttp3.OkHttpClient
import java.util.*
import java.util.prefs.Preferences
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideOkHttpClient() = OkHttpClient()

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
    fun provideLocale() = Locale.getDefault()

    @Provides
    fun provideCalendar() = Calendar.getInstance()

    @Provides
    fun provideChoreographer() = Choreographer.getInstance()
}
