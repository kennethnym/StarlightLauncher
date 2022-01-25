package kenneth.app.spotlightlauncher.extension

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.api.SearchModule
import kenneth.app.spotlightlauncher.api.SpotlightLauncherApi
import kenneth.app.spotlightlauncher.api.WidgetCreator
import kenneth.app.spotlightlauncher.api.intent.SpotlightLauncherIntent
import kenneth.app.spotlightlauncher.appsearchmodule.AppSearchModule
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExtensionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcherApi: SpotlightLauncherApi,
) {
    private val extensions = mutableMapOf<String, Extension>()

    private val searchModules = mutableMapOf<String, SearchModule>()

    val installedSearchModules = searchModules.values as Collection<SearchModule>

    init {
        listOf(
            "kenneth.app.spotlightlauncher" to Extension(
                packageName = "kenneth.app.spotlightlauncher",
                searchModule = AppSearchModule(),
            ),
        ).forEach { (name, ext) ->
            extensions[name] = ext
            ext.searchModule?.let { searchModules[name] = it }
        }
    }

    fun loadExtensions() {
        with(context.packageManager) {
            queryIntentActivities(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(SpotlightLauncherIntent.CATEGORY_EXTENSION)
                },
                PackageManager.GET_META_DATA,
            ).forEach { resolveInfo ->
                tryInitializeExtension(resolveInfo)
            }

            extensions.forEach { (_, ext) ->
                ext.searchModule?.initialize(launcherApi)
            }
        }
    }

    fun lookupSearchModule(extName: String) = extensions[extName]?.searchModule

    private fun tryInitializeExtension(resolveInfo: ResolveInfo) {
        try {
            val packageName = resolveInfo.activityInfo.packageName
            val packageRes =
                context.packageManager.getResourcesForApplication(packageName)

            val packageContext = context.createPackageContext(
                packageName,
                Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
            )

            val searchModuleClass =
                packageRes.getIdentifier("search_module_name", "string", packageName).let {
                    if (it == 0) null
                    else packageContext.classLoader.loadClass(
                        packageContext.getString(it)
                    )
                }

            val widgetClass =
                packageRes.getIdentifier("widget_name", "string", packageName).let {
                    if (it == 0) null
                    else packageContext.classLoader.loadClass(
                        packageContext.getString(it)
                    )
                }

            if (
                (searchModuleClass != null
                    && SearchModule::class.java.isAssignableFrom(searchModuleClass)) ||
                (widgetClass != null
                    && WidgetCreator::class.java.isAssignableFrom(widgetClass))
            ) {
                extensions[packageName] = Extension(
                    packageName,
                    searchModule = searchModuleClass?.newInstance() as SearchModule,
                    widget = widgetClass?.newInstance() as WidgetCreator
                )
            }
        } catch (ignored: Exception) {
            // invalid extension format, ignored.
        }
    }
}