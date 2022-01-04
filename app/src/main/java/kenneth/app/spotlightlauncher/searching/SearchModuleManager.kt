package kenneth.app.spotlightlauncher.searching

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.SpotlightLauncherApiImpl
import kenneth.app.spotlightlauncher.api.SearchModule
import kenneth.app.spotlightlauncher.api.SpotlightLauncherApi
import kenneth.app.spotlightlauncher.api.intent.SpotlightLauncherIntent
import kenneth.app.spotlightlauncher.appsearchmodule.AppSearchModule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchModuleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val spotlightLauncherApi: SpotlightLauncherApi,
) {
    private val modules = mutableMapOf<String, SearchModule>(
        "kenneth.app.spotlightlauncher.appsearchmodule" to AppSearchModule()
    )

    private val metadata = mutableMapOf<String, SearchModuleMetadata>(
        "kenneth.app.spotlightlauncher.appsearchmodule" to SearchModuleMetadata(
            name = context.getString(R.string.app_search_module_name),
            displayName = context.getString(R.string.app_search_module_display_name),
            description = context.getString(R.string.app_search_module_description),
        )
    )

    /**
     * Lists all installed search modules in a map. The keys are names of the installed [SearchModule]s,
     * and the values are the corresponding instance of the installed [SearchModule]s.
     */
    val installedSearchModules
        get() = modules.toMap()

    /**
     * Initializes all search modules.
     *
     * @param activityContext The activity context that the search module will be run in.
     */
    fun initializeModules(activityContext: Context) {
        if (spotlightLauncherApi is SpotlightLauncherApiImpl) {
            spotlightLauncherApi.context = activityContext
        }

        with(activityContext.packageManager) {
            queryIntentActivities(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(SpotlightLauncherIntent.CATEGORY_SEARCH_MODULE)
                },
                PackageManager.GET_META_DATA
            ).forEach { resolveInfo ->
                try {
                    val packageName = resolveInfo.activityInfo.packageName
                    val packageRes =
                        activityContext.packageManager.getResourcesForApplication(packageName)

                    val searchModuleName = packageRes.getString(
                        packageRes.getIdentifier("module_name", "string", packageName),
                    )

                    metadata[packageName] = SearchModuleMetadata(
                        name = searchModuleName,
                        displayName = packageRes.getString(
                            packageRes.getIdentifier("module_display_name", "string", packageName),
                        ),
                        description = packageRes.getString(
                            packageRes.getIdentifier("module_description", "string", packageName),
                        )
                    )

                    val clazz =
                        activityContext.createPackageContext(
                            packageName,
                            Context.CONTEXT_INCLUDE_CODE or Context.CONTEXT_IGNORE_SECURITY
                        )
                            .run {
                                classLoader.loadClass(searchModuleName)
                            }

                    if (SearchModule::class.java.isAssignableFrom(clazz)) {
                        modules[searchModuleName] = clazz.newInstance() as SearchModule
                    }
                } catch (ignored: Exception) {
                    // invalid plugins are ignored.
                }
            }

            queryIntentActivities(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(SpotlightLauncherIntent.CATEGORY_SEARCH_MODULE_SETTINGS)
                },
                PackageManager.GET_META_DATA,
            ).forEach { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val settingsActivityName = resolveInfo.activityInfo.name
                if (metadata.containsKey(packageName)) {
                    metadata[packageName]?.let {
                        metadata[packageName] = it.copy(
                            settingsActivityComponent = ComponentName(
                                packageName,
                                settingsActivityName
                            )
                        )
                    }
                }
            }
        }

        modules.forEach { (_, module) -> module.initialize(spotlightLauncherApi) }
    }

    fun lookupSearchModule(name: String) = modules[name]

    fun getSearchModuleMetadata(name: String) = metadata[name]
}