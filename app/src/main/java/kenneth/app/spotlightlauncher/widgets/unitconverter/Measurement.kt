package kenneth.app.spotlightlauncher.widgets.unitconverter

/**
 * Describes the value and the unit of a measurement.
 */
data class Measurement(
    val value: Double,
    val unit: MeasurementUnit,
) {
    /**
     * Converts this [Measurement] to another [Measurement] in the given unit.
     *
     * @param newUnit The [MeasurementUnit] the new [Measurement] should be in.
     * @return The new [Measurement] in the new unit, or null if the conversion is unsupported.
     */
    fun convertTo(newUnit: MeasurementUnit) =
        conversionFactors[unit]?.get(newUnit)?.let { factor ->
            Measurement(
                value = value * factor,
                unit = newUnit,
            )
        }
}
