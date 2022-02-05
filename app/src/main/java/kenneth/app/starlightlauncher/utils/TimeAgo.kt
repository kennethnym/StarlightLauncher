package kenneth.app.starlightlauncher.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.prefs.datetime.DateTimePreferenceManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Handles prettifying of dates.
 */
class TimeAgo @Inject constructor(
    @ActivityContext private val context: Context,
    private val dateTimePreferenceManager: DateTimePreferenceManager
) {
    private lateinit var now: LocalDateTime

    fun prettify(time: LocalDateTime): String {
        val is24hrFormat = dateTimePreferenceManager.shouldUse24HrClock

        val timeFormatPattern =
            if (is24hrFormat) "HH:mm"
            else "K:mm a"
        val timeFormatter = DateTimeFormatter.ofPattern(timeFormatPattern)
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy $timeFormatPattern")

        val formattedTime = timeFormatter.format(time)

        now = LocalDateTime.now()

        val dayDifference = ChronoUnit.DAYS.between(time, now)

        return when {
            isToday(time) -> context.getString(R.string.time_ago_today, formattedTime)
            isYesterday(time) -> context.getString(R.string.time_ago_yesterday, formattedTime)
            dayDifference <= 3 ->
                context.resources.getQuantityString(
                    R.plurals.time_ago_days_ago,
                    dayDifference.toInt(),
                    formattedTime,
                    dayDifference
                )
            else -> dateTimeFormatter.format(time)
        }
    }

    private fun isToday(time: LocalDateTime): Boolean =
        time.year == now.year &&
            time.month.value == now.month.value &&
            time.dayOfMonth == now.dayOfMonth

    private fun isYesterday(time: LocalDateTime): Boolean =
        time.year == now.year &&
            time.month.value == now.month.value &&
            now.dayOfMonth - time.dayOfMonth == 1
}