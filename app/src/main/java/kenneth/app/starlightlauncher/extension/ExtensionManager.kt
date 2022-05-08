package kenneth.app.starlightlauncher.extension

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.appcompat.content.res.AppCompatResources
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.SearchModule
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.WidgetCreator
import kenneth.app.starlightlauncher.api.intent.StarlightLauncherIntent
import kenneth.app.starlightlauncher.api.res.StarlightLauncherStringRes
import kenneth.app.starlightlauncher.prefs.searching.SearchPreferenceManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and manages Starlight Launcher extensions.
 */
@Singleton
internal class ExtensionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val searchPreferenceManager: SearchPreferenceManager,
    private val launcherApi: StarlightLauncherApi,
) {
    /**
     * A map of extensions loaded into memory.
     */
    private val extensions = mutableMapOf<String, Extension>().also {
        it += DEFAULT_EXTENSIONS
    }

    private val widgets = mutableMapOf<String, WidgetCreator>()

    private val widgetMetadata = mutableMapOf<String, WidgetMetadata>(
        "kenneth.app.starlightlauncher.appsearchmodule" to WidgetMetadata(
            extensionName = "kenneth.app.starlightlauncher.appsearchmodule",
            displayName = context.getString(R.string.pinned_apps_widget_display_name),
            description = context.getString(R.string.pinned_apps_widget_description),
        ),
        "kenneth.app.starlightlauncher.noteswidget" to WidgetMetadata(
            extensionName = "kenneth.app.starlightlauncher.noteswidget",
            displayName = context.getString(R.string.notes_widget_display_name),
            description = context.getString(R.string.notes_widget_settings_description)
        ),
        "kenneth.app.starlightlauncher.unitconverterwidget" to WidgetMetadata(
            extensionName = "kenneth.app.starlightlauncher.unitconverterwidget",
            displayName = context.getString(R.string.unit_converter_widget_display_name),
            description = context.getString(R.string.unit_converter_widget_description),
        )
    )

    private val searchModules = mutableMapOf<String, SearchModule>()

    /**
     * Stores all intents for settings activities exported by extensions.
     * Maps categories of settings, to a map of extension names to the corresponding
     * [Intent] to open the settings activity of that category exported by the extension.
     */
    private val extensionSettingsIntents = mutableMapOf(
        StarlightLauncherIntent.CATEGORY_SEARCH_MODULE_SETTINGS to mutableMapOf(
            "kenneth.app.starlightlauncher.appsearchmodule" to ExtensionSettings(
                title = context.getString(R.string.app_search_module_search_module_settings_title),
                description = context.getString(R.string.app_search_module_search_module_settings_description),
                icon = AppCompatResources.getDrawable(
                    context,
                    R.drawable.app_search_module_search_module_settings_icon
                ),
                intent = Intent(
                    context,
                    kenneth.app.starlightlauncher.appsearchmodule.activity.SearchModuleSettingsActivity::class.java
                )
            ),
            "kenneth.app.starlightlauncher.filesearchmodule" to ExtensionSettings(
                title = context.getString(R.string.file_search_module_search_module_settings_title),
                description = context.getString(R.string.file_search_module_search_module_settings_description),
                icon = AppCompatResources.getDrawable(
                    context,
                    R.drawable.file_search_module_search_module_settings_icon
                ),
                intent = Intent(
                    context,
                    kenneth.app.starlightlauncher.filesearchmodule.activity.SearchModuleSettingsActivity::class.java
                )
            )
        ),
        StarlightLauncherIntent.CATEGORY_WIDGET_SETTINGS to mutableMapOf(
            "kenneth.app.starlightlauncher.appsearchmodule" to ExtensionSettings(
                title = context.getString(R.string.app_search_module_widget_settings_title),
                icon = AppCompatResources.getDrawable(
                    context,
                    R.drawable.app_search_module_widget_settings_icon,
                ),
                intent = Intent(
                    context,
                    kenneth.app.starlightlauncher.appsearchmodule.activity.WidgetSettingsActivity::class.java,
                )
            ),
            "kenneth.app.starlightlauncher.noteswidget" to ExtensionSettings(
                title = context.getString(R.string.notes_widget_settings_title),
                description = context.getString(R.string.notes_widget_settings_description),
                icon = AppCompatResources.getDrawable(
                    context,
                    R.drawable.notes_widget_settings_icon,
                ),
                intent = Intent(
                    context,
                    kenneth.app.starlightlauncher.noteswidget.activity.WidgetSettingsActivity::class.java,
                )
            )
        )
    )

    private val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    val installedExtensions
        get() = extensions.values as Collection<Extension>

    val installedSearchModules = searchModules.values as Collection<SearchModule>

    val installedWidgets = widgets.values as Collection<WidgetCreator>

    init {
        extensions.forEach { (name, ext) ->
            extensions[name] = ext
            ext.searchModule?.let { searchModules[name] = it }
            ext.widget?.let { widgets[name] = it }
        }
    }

    fun loadExtensions() {
        queryExtensions()
        queryExtensionSettings()
    }

    /**
     * Determines whether an extension provides a [SearchModule].
     *
     * @param extName The name of the extension.
     */
    fun hasSearchModule(extName: String) = lookupSearchModule(extName) != null

    fun lookupWidget(extName: String) = extensions[extName]?.widget

    fun lookupSearchModule(extName: String) = extensions[extName]?.searchModule

    fun getIntentsForSettingsCategory(category: String) =
        extensionSettingsIntents[category]?.values?.toList() ?: listOf()

    private fun queryExtensions() {
        context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(StarlightLauncherIntent.CATEGORY_EXTENSION)
            },
            PackageManager.GET_META_DATA,
        ).forEach { resolveInfo ->
            tryInitializeExtension(resolveInfo)
        }

        // when the launcher is initializing, there may be new modules installed
        // since the last time the search module order list is saved to storage.
        //
        // therefore, we need to find out what modules are newly installed since last save,
        // add them to the order list and save the new list.
        //
        // SearchPreferenceManager will load the search module order list from storage
        // when initializing. we obtain the list from it, then update the list accordingly.

        val savedModules = sharedPreferences.getString(
            context.getString(R.string.pref_key_search_category_order),
            null,
        )

        extensions.forEach { (_, ext) ->
            if (savedModules != null && savedModules.indexOf(ext.name) < 0) {
                searchPreferenceManager.addNewSearchModule(ext.name)
            }

            ext.searchModule?.initialize(launcherApi)
        }
    }

    /**
     * Query settings activities exported by extensions.
     */
    private fun queryExtensionSettings() {
        // query activities for each settings categories
        listOf(
            StarlightLauncherIntent.CATEGORY_WIDGET_SETTINGS,
            StarlightLauncherIntent.CATEGORY_SEARCH_MODULE_SETTINGS,
        ).forEach { settingsCategory ->
            context.packageManager.queryIntentActivities(
                Intent(Intent.ACTION_MAIN).apply {
                    addCategory(settingsCategory)
                },
                PackageManager.GET_META_DATA,
            ).forEach { resolveInfo ->
                try {
                    // for each settings activity resolved for this specific
                    // settings category, create an intent for launching it

                    val packageName = resolveInfo.activityInfo.packageName
                    val packageRes =
                        context.packageManager.getResourcesForApplication(packageName)

                    val extensionSettings = ExtensionSettings(
                        title = packageRes.getString(
                            packageRes.getIdentifier(
                                StarlightLauncherStringRes.SEARCH_MODULE_SETTINGS_TITLE,
                                "string",
                                packageName
                            )
                        ),
                        description = packageRes.getString(
                            packageRes.getIdentifier(
                                StarlightLauncherStringRes.SEARCH_MODULE_SETTINGS_DESCRIPTION,
                                "string",
                                packageName
                            )
                        ),
                        icon = resolveInfo.activityInfo.loadIcon(context.packageManager),
                        intent = Intent(Intent.ACTION_MAIN).apply {
                            `package` = packageName
                            addCategory(settingsCategory)
                        }
                    )

                    // save this intent
                    extensionSettingsIntents[settingsCategory]
                        // map of intents for this category already exists,
                        // save this intent to the map
                        ?.let { it[packageName] = extensionSettings }
                        ?: run {
                            // map of intents for this category doesn't exists yet,
                            // create a new map of intents for this category
                            extensionSettingsIntents[settingsCategory] = mutableMapOf(
                                packageName to extensionSettings
                            )
                        }
                } catch (ignored: Exception) {
                    // invalid extension format, ignore
                }
            }
        }
    }

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
                packageRes.getIdentifier(
                    StarlightLauncherStringRes.SEARCH_MODULE_NAME,
                    "string",
                    packageName
                ).let {
                    if (it == 0) null
                    else packageContext.classLoader.loadClass(
                        packageContext.getString(it)
                    )
                }

            val widgetClass =
                packageRes.getIdentifier(
                    StarlightLauncherStringRes.WIDGET_NAME,
                    "string",
                    packageName
                ).let {
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