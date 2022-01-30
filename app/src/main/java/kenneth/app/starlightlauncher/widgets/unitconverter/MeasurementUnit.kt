package kenneth.app.starlightlauncher.widgets.unitconverter

import kenneth.app.starlightlauncher.utils.ContextMenuEntry

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
    KILOMETERS(7, "Kilometers"),

    // Weight units
    // ===========
    // Imperial units
    OUNCES(8, "Ounces"),
    POUND(9, "Pound"),
    SHORT_TONS(10, "Short tons"),
    LONG_TONS(11, "Long tons"),

    // Metric units
    MILLIGRAMS(12, "Milligrams"),
    GRAMS(13, "Grams"),
    KILOGRAMS(14, "Kilograms"),
    METRIC_TONS(15, "Metric tons");

    companion object {
        fun fromId(id: Int) = values().first { it.id == id }
    }
}
