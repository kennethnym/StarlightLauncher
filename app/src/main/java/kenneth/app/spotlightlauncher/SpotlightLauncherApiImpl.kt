package kenneth.app.spotlightlauncher

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityScoped
import dagger.hilt.components.SingletonComponent
import kenneth.app.spotlightlauncher.api.IconPack
import kenneth.app.spotlightlauncher.api.SpotlightLauncherApi
import kenneth.app.spotlightlauncher.api.view.OptionMenuBuilder
import kenneth.app.spotlightlauncher.prefs.appearance.AppearancePreferenceManager
import kenneth.app.spotlightlauncher.utils.BindingRegister
import javax.inject.Inject
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SpotlightLauncherApiModule {
    @Binds
    abstract fun bindSpotlightLauncherApi(
        impl: SpotlightLauncherApiImpl
    ): SpotlightLauncherApi
}

@Singleton
class SpotlightLauncherApiImpl @Inject constructor(
    private val appearancePreferenceManager: AppearancePreferenceManager,
) : SpotlightLauncherApi {
    override lateinit var context: Context
        internal set

    override fun showOptionMenu(builder: OptionMenuBuilder) {
        BindingRegister.activityMainBinding.optionMenu.show(builder)
    }

    override fun getIconPack(): IconPack = appearancePreferenceManager.iconPack
}