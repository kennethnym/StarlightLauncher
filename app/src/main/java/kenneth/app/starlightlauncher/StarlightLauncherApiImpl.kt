package kenneth.app.starlightlauncher

import android.content.Context
import android.view.View
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kenneth.app.starlightlauncher.api.IconPack
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.utils.BlurHandler
import kenneth.app.starlightlauncher.api.view.OptionMenuBuilder
import kenneth.app.starlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.starlightlauncher.utils.BindingRegister
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class StarlightLauncherApiModule {
    @Binds
    abstract fun bindStarlightLauncherApi(
        impl: StarlightLauncherApiImpl
    ): StarlightLauncherApi
}

@Singleton
class StarlightLauncherApiImpl @Inject constructor(
    private val appearancePreferenceManager: AppearancePreferenceManager,
    override val blurHandler: BlurHandler
) : StarlightLauncherApi {
    override lateinit var context: Context
        internal set

    override fun showOptionMenu(builder: OptionMenuBuilder) {
        BindingRegister.activityMainBinding.optionMenu.show(builder)
    }

    override fun closeOptionMenu() {
        BindingRegister.activityMainBinding.optionMenu.hide()
    }

    override fun showOverlay(fromView: View, viewConstructor: (context: Context) -> View) {
        BindingRegister.activityMainBinding.overlay.showFrom(fromView, viewConstructor(context))
    }

    override fun closeOverlay() {
        BindingRegister.activityMainBinding.overlay.close()
    }

    override fun getIconPack(): IconPack = appearancePreferenceManager.iconPack
}