package kenneth.app.spotlightlauncher.widgets.unitconverter

import kenneth.app.spotlightlauncher.utils.ContextMenuEntry

/**
 * Supported measurements that [UnitConverterWidget] can provide conversions for.
 */
enum class MeasurementType(
    override val id: Int,
    override val label: String
) : ContextMenuEntry {
    LENGTH(0, "Length"),
    WEIGHT(1, "Weight");

    companion object {
        fun fromId(id: Int) = values().first { it.id == id }
    }
}