package kenneth.app.spotlightlauncher.widgets.unitconverter

import android.icu.util.Measure

val conversionFactors: Map<MeasurementUnit, Map<MeasurementUnit, Double>> = mapOf(
    // length
    // ======
    MeasurementUnit.MILLIMETERS to mapOf(
        MeasurementUnit.CENTIMETERS to 0.1,
        MeasurementUnit.METERS to 0.001,
        MeasurementUnit.KILOMETERS to 0.000001,
        MeasurementUnit.INCHES to 0.0393701,
        MeasurementUnit.FOOT to 0.00328084,
        MeasurementUnit.YARDS to 0.00109361,
        MeasurementUnit.MILES to 0.00000062137,
    ),
    MeasurementUnit.CENTIMETERS to mapOf(
        MeasurementUnit.MILLIMETERS to 10.0,
        MeasurementUnit.METERS to 0.01,
        MeasurementUnit.KILOMETERS to 0.00001,
        MeasurementUnit.INCHES to 0.3937007874,
        MeasurementUnit.FOOT to 0.032808399,
        MeasurementUnit.YARDS to 0.010936133,
        MeasurementUnit.MILES to 0.0000062137,
    ),
    MeasurementUnit.METERS to mapOf(
        MeasurementUnit.MILLIMETERS to 1000.0,
        MeasurementUnit.CENTIMETERS to 100.0,
        MeasurementUnit.KILOMETERS to 0.001,
        MeasurementUnit.INCHES to 39.37007874,
        MeasurementUnit.FOOT to 3.280839895,
        MeasurementUnit.YARDS to 1.0936132983,
        MeasurementUnit.MILES to 0.0006213712,
    ),
    MeasurementUnit.KILOMETERS to mapOf(
        MeasurementUnit.MILLIMETERS to 1000000.0,
        MeasurementUnit.CENTIMETERS to 100000.0,
        MeasurementUnit.METERS to 1000.0,
        MeasurementUnit.INCHES to 39370.07874,
        MeasurementUnit.FOOT to 3280.839895,
        MeasurementUnit.YARDS to 1093.6132983,
        MeasurementUnit.MILES to 0.6213711922,
    ),
    MeasurementUnit.INCHES to mapOf(
        MeasurementUnit.MILLIMETERS to 25.4,
        MeasurementUnit.CENTIMETERS to 2.54,
        MeasurementUnit.METERS to 0.0254,
        MeasurementUnit.KILOMETERS to 0.0000254,
        MeasurementUnit.FOOT to 0.0833333333,
        MeasurementUnit.YARDS to 0.0277777778,
        MeasurementUnit.MILES to 0.0000157828,
    ),
    MeasurementUnit.FOOT to mapOf(
        MeasurementUnit.MILLIMETERS to 304.8,
        MeasurementUnit.CENTIMETERS to 30.48,
        MeasurementUnit.METERS to 0.3048,
        MeasurementUnit.KILOMETERS to 0.0003048,
        MeasurementUnit.INCHES to 12.0,
        MeasurementUnit.YARDS to 0.3333333333,
        MeasurementUnit.MILES to 0.0001893939,
    ),
    MeasurementUnit.YARDS to mapOf(
        MeasurementUnit.MILLIMETERS to 914.4,
        MeasurementUnit.CENTIMETERS to 91.44,
        MeasurementUnit.METERS to 0.9144,
        MeasurementUnit.KILOMETERS to 0.0009144,
        MeasurementUnit.INCHES to 36.0,
        MeasurementUnit.FOOT to 3.0,
        MeasurementUnit.MILES to 0.0005681818,
    ),
    MeasurementUnit.MILES to mapOf(
        MeasurementUnit.MILLIMETERS to 1609344.0,
        MeasurementUnit.CENTIMETERS to 160934.4,
        MeasurementUnit.METERS to 1609.344,
        MeasurementUnit.KILOMETERS to 1.609344,
        MeasurementUnit.INCHES to 63360.0,
        MeasurementUnit.FOOT to 5280.0,
        MeasurementUnit.YARDS to 1760.0,
    ),

    // weight
    // =====
    MeasurementUnit.OUNCES to mapOf(
        MeasurementUnit.POUND to 0.0625,
        MeasurementUnit.SHORT_TONS to 0.00003125,
        MeasurementUnit.LONG_TONS to 0.0000279018,
        MeasurementUnit.MILLIGRAMS to 28349.5,
        MeasurementUnit.GRAMS to 28.3495,
        MeasurementUnit.KILOGRAMS to 0.0283495,
        MeasurementUnit.METRIC_TONS to 0.0000283495,
    ),
    MeasurementUnit.POUND to mapOf(
        MeasurementUnit.OUNCES to 16.0,
        MeasurementUnit.SHORT_TONS to 0.00005,
        MeasurementUnit.LONG_TONS to 0.0004464286,
        MeasurementUnit.MILLIGRAMS to 453592.0,
        MeasurementUnit.GRAMS to 453.592,
        MeasurementUnit.KILOGRAMS to 0.453592,
        MeasurementUnit.METRIC_TONS to 0.000453592,
    ),
    MeasurementUnit.SHORT_TONS to mapOf(
        MeasurementUnit.OUNCES to 32000.0,
        MeasurementUnit.POUND to 2000.0,
        MeasurementUnit.LONG_TONS to 0.8928571429,
        MeasurementUnit.MILLIGRAMS to 9071840000.0,
        MeasurementUnit.GRAMS to 907184.0,
        MeasurementUnit.KILOGRAMS to 907.184,
        MeasurementUnit.METRIC_TONS to 0.907184,
    ),
    MeasurementUnit.LONG_TONS to mapOf(
        MeasurementUnit.OUNCES to 35840.0,
        MeasurementUnit.POUND to 2240.0,
        MeasurementUnit.SHORT_TONS to 1.12,
        MeasurementUnit.MILLIGRAMS to 1016046080.0,
        MeasurementUnit.GRAMS to 1016046.08,
        MeasurementUnit.KILOGRAMS to 1016.04608,
        MeasurementUnit.METRIC_TONS to 1.01604608,
    ),
    MeasurementUnit.MILLIGRAMS to mapOf(
        MeasurementUnit.OUNCES to 0.000035274,
        MeasurementUnit.POUND to 0.0000022046,
        MeasurementUnit.SHORT_TONS to 0.00000000110231221,
        MeasurementUnit.LONG_TONS to 0.0000000009842073304,
        MeasurementUnit.GRAMS to 0.001,
        MeasurementUnit.KILOGRAMS to 0.000001,
        MeasurementUnit.METRIC_TONS to 0.0000000009999999999,
    ),
    MeasurementUnit.GRAMS to mapOf(
        MeasurementUnit.OUNCES to 0.0352739907,
        MeasurementUnit.POUND to 0.0022046244,
        MeasurementUnit.SHORT_TONS to 0.0000011023,
        MeasurementUnit.LONG_TONS to 0.0000009842073304,
        MeasurementUnit.MILLIGRAMS to 1000.0,
        MeasurementUnit.KILOGRAMS to 0.001,
        MeasurementUnit.METRIC_TONS to 0.000001,
    ),
    MeasurementUnit.KILOGRAMS to mapOf(
        MeasurementUnit.OUNCES to 35.273990723,
        MeasurementUnit.POUND to 2.2046244202,
        MeasurementUnit.SHORT_TONS to 0.0011023122,
        MeasurementUnit.LONG_TONS to 0.0009842073,
        MeasurementUnit.MILLIGRAMS to 1000000.0,
        MeasurementUnit.GRAMS to 1000.0,
        MeasurementUnit.METRIC_TONS to 0.001,
    ),
    MeasurementUnit.METRIC_TONS to mapOf(
        MeasurementUnit.OUNCES to 35273.990723,
        MeasurementUnit.POUND to 2204.6244202,
        MeasurementUnit.SHORT_TONS to 1.1023122101,
        MeasurementUnit.LONG_TONS to 0.9842073304,
        MeasurementUnit.MILLIGRAMS to 1000000000.0,
        MeasurementUnit.GRAMS to 1000000.0,
        MeasurementUnit.KILOGRAMS to 1000000.0,
    )
)
