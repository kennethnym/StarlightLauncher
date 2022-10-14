package kenneth.app.starlightlauncher.datetime

/**
 * Defines different sizes of [DateTimeView]
 */
internal enum class DateTimeViewSize(
    /**
     * Size of the clock in sp.
     */
    val clockSize: Int,

    /**
     * Size of the date in sp.
     */
    val dateSize: Int,
) {
    GIGA_SMALL(clockSize = 16, dateSize = 8),
    SMALL(clockSize = 24, dateSize = 10),
    NORMAL(clockSize = 32, dateSize = 16),
    BIG(clockSize = 40, dateSize = 18),
    GIGA_BIG(clockSize = 48, dateSize = 24),
}