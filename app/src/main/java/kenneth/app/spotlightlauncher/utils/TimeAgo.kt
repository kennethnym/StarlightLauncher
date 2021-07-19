package kenneth.app.spotlightlauncher.utils

import android.content.Context
import dagger.hilt.android.qualifiers.ActivityContext
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.prefs.datetime.DateTimePreferenceManager
import kotlinx.datetime.*
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.ExperimentalTime

/**
 * Handles prettifying of dates.
 */
class TimeAgo @Inject constructor(
    @ActivityContext private val context: Context,
    private val dateTimePreferenceManager: DateTimePreferenceManager
) {
    private lateinit var now: LocalDateTime

    @ExperimentalTime
    fun prettify(time: Instant): String {
        val timezone = TimeZone.currentSystemDefault()
        val dateTime = time.toLocalDateTime(timezone)
        val dateTimeJava = dateTime.toJavaLocalDateTime()
        val is24hrFormat = dateTimePreferenceManager.shouldUse24HrClock

        val timeFormatPattern =
            if (is24hrFormat) "HH:mm"
            else "K:mm a"
        val timeFormatter = DateTimeFormatter.ofPattern(timeFormatPattern)
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy $timeFormatPattern")

        val formattedTime = timeFormatter.format(dateTimeJava)

        val nowInstant = Clock.System.now()
        now = nowInstant.toLocalDateTime(timezone)

        val dayDifference = (nowInstant - time).inWholeDays

        return when {
            isToday(dateTime) -> context.getString(R.string.time_ago_today, formattedTime)
            isYesterday(dateTime) -> context.getString(R.string.time_ago_yesterday, formattedTime)
            dayDifference <= 3 ->
                context.resources.getQuantityString(
                    R.plurals.time_ago_days_ago,
                    dayDifference.toInt(),
                    formattedTime,
                    dayDifference
                )
            else -> dateTimeFormatter.format(dateTimeJava)
        }
    }

    private fun isToday(time: LocalDateTime): Boolean =
        time.year == now.year &&
            time.monthNumber == now.monthNumber &&
            time.dayOfMonth == now.dayOfMonth

    private fun isYesterday(time: LocalDateTime): Boolean =
        time.year == now.year &&
            time.monthNumber == now.monthNumber &&
            now.dayOfMonth - time.dayOfMonth == 1
}