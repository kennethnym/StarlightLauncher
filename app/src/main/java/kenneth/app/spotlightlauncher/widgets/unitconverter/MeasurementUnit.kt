package kenneth.app.spotlightlauncher.widgets.unitconverter

import kenneth.app.spotlightlauncher.utils.ContextMenuEntry

/**
 * Defines units of measurements.
 */
enum class MeasurementUnit(
    override val id: Int,
    override val label: String
) : ContextMenuEntry {
    // Length units
    // ============
    // Imperial units
    INCHES(0, "Inches"),
    FOOT(1, "Foot"),
    YARDS(2, "Yards"),
    MILES(3, "Miles"),

    // Metric units
    MILLIMETERS(4, "Millimeters"),
    CENTIMETERS(5, "Centimeters"),
    METERS(6, "Meters"),
    KILOMETERS(7, "Kilometers");

    companion object {
        fun fromId(id: Int) = values().first { it.id == id }
    }
}
