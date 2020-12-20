package kenneth.app.spotlightlauncher

import android.content.Context
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.view.Choreographer
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.prefs.PinnedAppsPreferenceManager
import kenneth.app.spotlightlauncher.prefs.files.FilePreferenceManager
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

    @Provides
    fun provideMediaSessionManager(@ActivityContext context: Context) =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
}
