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
import kenneth.app.starlightlauncher.appsearchmodule.AppSearchModule
import kenneth.app.starlightlauncher.appsearchmodule.widget.PinnedAppsWidgetCreator
import kenneth.app.starlightlauncher.appshortcutsearchmodule.AppShortcutSearchModule
import kenneth.app.starlightlauncher.contactsearchmodule.ContactSearchModule
import kenneth.app.starlightlauncher.filesearchmodule.FileSearchModule
import kenneth.app.starlightlauncher.mathsearchmodule.MathSearchModule
import kenneth.app.starlightlauncher.noteswidget.NotesWidgetCreator
import kenneth.app.starlightlauncher.unitconverterwidget.UnitConverterWidgetCreator
import kenneth.app.starlightlauncher.urlopener.UrlOpener
import kenneth.app.starlightlauncher.wificontrolmodule.WifiControlModule
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private sealed class ExtensionManagerEvent {
    data class ExtensionsLoaded(val extensions: List<Extension>)
}

/**
 * Loads and manages Starlight Launcher extensions.
 */
@Singleton
internal class ExtensionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcherApi: StarlightLauncherApi,
) {
    /**
     * An observable that listeners of [ExtensionManager] is subscribed to.
     */
    private val observable = object : Observable() {
        fun changed() {
            setChanged()
        }
    }

    /**
     * A map of extensions loaded into memory.
     */
    private val extensions = mutableMapOf(
        "kenneth.app.starlightlauncher.appsearchmodule" to Extension(
            name = "kenneth.app.starlightlauncher.appsearchmodule",
            searchModule = AppSearchModule(context),
            widget = PinnedAppsWidgetCreator(context),
        ),
        "kenneth.app.starlightlauncher.appshortcutsearchmodule" to Extension(
            name = "kenneth.app.starlightlauncher.appshortcutsearchmodule",
            searchModule = AppShortcutSearchModule(context),
        ),
        "kenneth.app.starlightlauncher.contactsearchmodule" to Extension(
            name = "kenneth.app.starlightlauncher.contactsearchmodule",
            searchModule = ContactSearchModule(context),
        ),
        "kenneth.app.starlightlauncher.filesearchmodule" to Extension(
            name = "kenneth.app.starlightlauncher.filesearchmodule",
            searchModule = FileSearchModule(context),
        ),
        "kenneth.app.starlightlauncher.mathsearchmodule" to Extension(
            name = "kenneth.app.starlightlauncher.mathsearchmodule",
            searchModule = MathSearchModule(context),
        ),
        "kenneth.app.starlightlauncher.wificontrolmodule" to Extension(
            name = "kenneth.app.starlightlauncher.wificontrolmodule",
            searchModule = WifiControlModule(context),
        ),
        "kenneth.app.starlightlauncher.urlopener" to Extension(
            name = "kenneth.app.starlightlauncher.urlopener",
            searchModule = UrlOpener(context),
        ),
        "kenneth.app.starlightlauncher.noteswidget" to Extension(
            name = "kenneth.app.starlightlauncher.noteswidget",
            widget = NotesWidgetCreator(context),
        ),
        "kenneth.app.starlightlauncher.unitconverterwidget" to Extension(
            name = "kenneth.app.starlightlauncher.unitconverterwidget",
            widget = UnitConverterWidgetCreator(context),
        ),
    )

    private val widgets = mutableMapOf<String, WidgetCreator>()

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
        with(observable) {
            changed()
            notifyObservers()
        }
    }

    fun addOnExtensionsLoadedListener(listener: (extensions: List<Extension>) -> Unit) {
        observable.addObserver { o, arg ->
            if (arg is ExtensionManagerEvent.ExtensionsLoaded) {
                listener(arg.extensions)
            }
        }
    }

    fun isExtensionInstalled(extName: String) = extensions.containsKey(extName)

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
                    searchModule = searchModuleClass
                        ?.getDeclaredConstructor(Context::class.java)!!
                        .newInstance() as SearchModule,
                    widget = widgetClass?.newInstance() as WidgetCreator
                )
            }
        } catch (ignored: Exception) {
            // invalid extension format, ignored.
        }
    }
}