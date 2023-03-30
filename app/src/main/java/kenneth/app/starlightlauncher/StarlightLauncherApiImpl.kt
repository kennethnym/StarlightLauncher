package kenneth.app.starlightlauncher

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import kenneth.app.starlightlauncher.api.*
import kenneth.app.starlightlauncher.api.util.BlurHandler
import kenneth.app.starlightlauncher.api.view.OptionMenuBuilder
import kenneth.app.starlightlauncher.prefs.appearance.AppearancePreferenceManager
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@Module
@InstallIn(ActivityComponent::class)
internal abstract class StarlightLauncherApiModule {
    @Binds
    abstract fun bindStarlightLauncherApi(
        impl: StarlightLauncherApiImpl
    ): StarlightLauncherApi
}

/**
 * An implementation of [StarlightLauncherApi] which extensions can use to provide
 * additional features for the launcher.
 */
@ActivityScoped
internal class StarlightLauncherApiImpl @Inject constructor(
    @ActivityContext private val activityContext: Context,
    appearancePreferenceManager: AppearancePreferenceManager,
    private val launcherEventChannel: LauncherEventChannel,
    private val bindingRegister: BindingRegister,
    private val appManager: AppManager,
    override val coroutineScope: CoroutineScope,
    override val blurHandler: BlurHandler,
) : StarlightLauncherApi {
    private val requestPermissionLauncher =
        (activityContext as ComponentActivity).registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { result ->
            requestPermissionResultCallback?.let {
                it(result)
                requestPermissionResultCallback = null
            }
        }

    private val requestMultiplePermissionsLauncher =
        (activityContext as ComponentActivity).registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) { result ->
            requestMultiplePermissionsResultCallback?.let {
                it(result)
                requestMultiplePermissionsResultCallback = null
            }
        }

    private var requestPermissionResultCallback: PermissionRequestCallback? = null

    private var requestMultiplePermissionsResultCallback: MultiplePermissionRequestCallback? = null

    override val dataStore = activityContext.dataStore

    override val iconPack = appearancePreferenceManager.iconPack

    override val context = activityContext

    override val installedApps: List<LauncherActivityInfo>
        get() = appManager.installedApps

    override fun appLabelOf(packageName: String): String? =
        appManager.appLabelOf(packageName)

    override fun launcherActivityInfoOf(componentName: ComponentName): LauncherActivityInfo? =
        appManager.launcherActivityInfoOf(componentName)

    override fun showOptionMenu(builder: OptionMenuBuilder) {
        if (activityContext is MainActivity) {
            activityContext.showOptionMenu(builder)
        }
    }

    override fun closeOptionMenu() {
        if (activityContext is MainActivity) {
            activityContext.closeOptionMenu()
        }
    }

    override fun showOverlay(fromView: View, viewConstructor: (context: Context) -> View) {
//        bindingRegister.mainScreenBinding.overlay.showFrom(fromView, viewConstructor(context))
    }

    override fun showOverlay(fragment: Fragment) {
        if (activityContext is MainActivity) {
            activityContext.showOverlay(fragment)
        }
    }

    override fun closeOverlay() {
        if (activityContext is MainActivity) {
            activityContext.closeOverlay()
        }
    }

    override fun requestPermission(permission: String, callback: PermissionRequestCallback) {
        requestPermissionResultCallback = callback
        requestPermissionLauncher.launch(permission)
    }

    override fun requestPermissions(
        vararg permissions: String,
        callback: MultiplePermissionRequestCallback
    ) {
        requestMultiplePermissionsResultCallback = callback
        requestMultiplePermissionsLauncher.launch(arrayOf(*permissions))
    }

    override suspend fun addLauncherEventListener(listener: LauncherEventListener) =
        launcherEventChannel.subscribePublic(listener)
}