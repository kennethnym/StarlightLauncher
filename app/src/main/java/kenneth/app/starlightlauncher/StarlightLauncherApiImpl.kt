package kenneth.app.starlightlauncher

import android.content.Context
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
abstract class SpotlightLauncherApiModule {
    @Binds
    abstract fun bindSpotlightLauncherApi(
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

    override fun getIconPack(): IconPack = appearancePreferenceManager.iconPack
}