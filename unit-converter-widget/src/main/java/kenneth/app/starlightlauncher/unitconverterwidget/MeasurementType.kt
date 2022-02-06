package kenneth.app.starlightlauncher.unitconverterwidget

/**
 * Supported measurements that [UnitConverterWidget] can provide conversions for.
 */
enum class MeasurementType(
    val id: Int,
    val label: String
) {
    LENGTH(0, "Length"),
    WEIGHT(1, "Weight");

    companion object {
        fun fromId(id: Int) = values().first { it.id == id }
    }
}