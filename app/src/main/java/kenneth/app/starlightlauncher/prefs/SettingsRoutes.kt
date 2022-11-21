package kenneth.app.starlightlauncher.prefs

const val SETTINGS_ROUTE_ROOT = "root"
const val SETTINGS_ROUTE_APPEARANCE = "appearance"
const val SETTINGS_ROUTE_CLOCK = "clock"
const val SETTINGS_ROUTE_ICON_PACK = "appearance/icon_pack"
const val SETTINGS_ROUTE_SEARCH = "search"
const val SETTINGS_ROUTE_SEARCH_LAYOUT = "search/layout"
const val SETTINGS_ROUTE_INFO = "info"

fun baseExtensionRoute(extensionName: String) = "extension/$extensionName"

fun rootExtensionRoute(extensionName: String) = "${baseExtensionRoute(extensionName)}/root"
