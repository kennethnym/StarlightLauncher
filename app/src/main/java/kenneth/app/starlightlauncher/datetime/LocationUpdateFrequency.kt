package kenneth.app.starlightlauncher.datetime

/**
 * Defines the possible values that can be used to set
 * how often the device location should be requested for fetching weather.
 * Only applies when user enables auto weather location.
 *
 * The numbers are in milliseconds.
 */
val LOCATION_UPDATE_FREQUENCY_VALUES = listOf(
    300000L,
    900000L,
    1800000L,
    3600000L,
    7200000L,
    18000000L,
    43200000L,
    86400000L
)
