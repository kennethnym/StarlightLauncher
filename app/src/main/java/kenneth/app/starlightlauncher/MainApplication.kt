package kenneth.app.starlightlauncher

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.google.android.material.color.DynamicColors
import dagger.hilt.android.HiltAndroidApp
import kenneth.app.starlightlauncher.searching.Searcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import javax.inject.Inject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "starlightLauncherSettings",
    produceMigrations = {
        listOf(SharedPreferencesMigration(it, "${it.packageName}_preferences"))
    }
)

// TODO: add support for gesture navigation animation
//
// copied from a commit to Launcher3
// Adding home animation support for non-system Launcher
//
// When user swipes up to home, Launcher will receive a onNewIntent
// callwith a bundle-extra gesture_nav_contract_v1. It will contain
// the componentName & UserHandle of the closing app & a callback.
// Launcher can use the callback to return the final position where
// the app should animate to and an optional surface to be used for
// crossFade animation. The surface cleanup can be handled in
// onEnterAnimationComplete.
//
// url: https://android.googlesource.com/platform/packages/apps/Launcher3/+/30ac97d9386736c3322a3277f74523497627b9f5

@HiltAndroidApp
internal class MainApplication : Application() {
    @Inject
    lateinit var applicationScope: CoroutineScope

    @Inject
    lateinit var searcher: Searcher

    override fun onCreate() {
        DynamicColors.applyToActivitiesIfAvailable(this)
        super.onCreate()
    }

    override fun onTerminate() {
        searcher.cancelPendingSearch()
        applicationScope.cancel()
        super.onTerminate()
    }
}