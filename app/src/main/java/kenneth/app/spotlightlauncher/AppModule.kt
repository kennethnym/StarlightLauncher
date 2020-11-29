package kenneth.app.spotlightlauncher

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.prefs.files.FilePreferenceManager
import okhttp3.OkHttpClient
import java.util.*
import javax.inject.Singleton

/**
 * AppState holds the current state of the application like theme.
 */
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
    fun provideLocale() = Locale.getDefault()
}
