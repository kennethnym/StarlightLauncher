package kenneth.app.starlightlauncher

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.multibindings.IntoMap
import kenneth.app.starlightlauncher.home.AppDrawerScreenFragment
import kenneth.app.starlightlauncher.home.MainScreenFragment
import javax.inject.Inject
import javax.inject.Provider
import kotlin.reflect.KClass

// credit to this medium article
// https://medium.com/supercharges-mobile-product-guide/fragmentfactory-with-dagger-and-hilt-31ee17babf73

@MapKey
@Retention(AnnotationRetention.RUNTIME)
internal annotation class FragmentKey(val value: KClass<out Fragment>)

@EntryPoint
@InstallIn(ActivityComponent::class)
internal interface LauncherFragmentFactoryEntryPoint {
    fun launcherFragmentFactory(): LauncherFragmentFactory
}

@Module
@InstallIn(ActivityComponent::class)
internal abstract class LauncherFragmentFactoryModule {
    @Binds
    @IntoMap
    @FragmentKey(MainScreenFragment::class)
    abstract fun bindMainScreenFragment(impl: MainScreenFragment): Fragment

    @Binds
    @IntoMap
    @FragmentKey(AppDrawerScreenFragment::class)
    abstract fun bindAppDrawerScreenFragment(impl: AppDrawerScreenFragment): Fragment
}

internal class LauncherFragmentFactory @Inject constructor(
    private val providerMap: Map<Class<out Fragment>, @JvmSuppressWildcards Provider<Fragment>>
) : FragmentFactory() {
    override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
        val fragmentClass = loadFragmentClass(classLoader, className)

        val creator = providerMap[fragmentClass] ?: providerMap.entries.firstOrNull {
            fragmentClass.isAssignableFrom(it.key)
        }?.value

        return creator?.get() ?: super.instantiate(classLoader, className)
    }
}