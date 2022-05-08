package kenneth.app.starlightlauncher.extension

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

// This file defines default extensions loaded by Starlight Launcher

internal val DEFAULT_EXTENSION_NAMES = listOf(
    "kenneth.app.starlightlauncher.appsearchmodule",
    "kenneth.app.starlightlauncher.appshortcutsearchmodule",
    "kenneth.app.starlightlauncher.contactsearchmodule",
    "kenneth.app.starlightlauncher.filesearchmodule",
    "kenneth.app.starlightlauncher.mathsearchmodule",
    "kenneth.app.starlightlauncher.wificontrolmodule",
    "kenneth.app.starlightlauncher.urlopener",
    "kenneth.app.starlightlauncher.noteswidget",
    "kenneth.app.starlightlauncher.unitconverterwidget",
)

internal val DEFAULT_EXTENSIONS = mapOf<String, Extension>(
    "kenneth.app.starlightlauncher.appsearchmodule" to Extension(
        name = "kenneth.app.starlightlauncher.appsearchmodule",
        searchModule = AppSearchModule(),
        widget = PinnedAppsWidgetCreator(),
    ),
    "kenneth.app.starlightlauncher.appshortcutsearchmodule" to Extension(
        name = "kenneth.app.starlightlauncher.appshortcutsearchmodule",
        searchModule = AppShortcutSearchModule(),
    ),
    "kenneth.app.starlightlauncher.contactsearchmodule" to Extension(
        name = "kenneth.app.starlightlauncher.contactsearchmodule",
        searchModule = ContactSearchModule(),
    ),
    "kenneth.app.starlightlauncher.filesearchmodule" to Extension(
        name = "kenneth.app.starlightlauncher.filesearchmodule",
        searchModule = FileSearchModule(),
    ),
    "kenneth.app.starlightlauncher.mathsearchmodule" to Extension(
        name = "kenneth.app.starlightlauncher.mathsearchmodule",
        searchModule = MathSearchModule(),
    ),
    "kenneth.app.starlightlauncher.wificontrolmodule" to Extension(
        name = "kenneth.app.starlightlauncher.wificontrolmodule",
        searchModule = WifiControlModule(),
    ),
    "kenneth.app.starlightlauncher.urlopener" to Extension(
        name = "kenneth.app.starlightlauncher.urlopener",
        searchModule = UrlOpener(),
    ),
    "kenneth.app.starlightlauncher.noteswidget" to Extension(
        name = "kenneth.app.starlightlauncher.noteswidget",
        widget = NotesWidgetCreator(),
    ),
    "kenneth.app.starlightlauncher.unitconverterwidget" to Extension(
        name = "kenneth.app.starlightlauncher.unitconverterwidget",
        widget = UnitConverterWidgetCreator(),
    ),
)
