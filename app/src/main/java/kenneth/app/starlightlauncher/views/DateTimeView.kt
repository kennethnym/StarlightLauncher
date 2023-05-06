package kenneth.app.starlightlauncher.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.OpenWeatherApi
import kenneth.app.starlightlauncher.api.view.IconButton
import kenneth.app.starlightlauncher.databinding.DateTimeViewBinding
import kenneth.app.starlightlauncher.datetime.*
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
internal class DateTimeView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    var dateTime: Date? = null
        set(date) {
            field = dateTime
            if (date != null) {
                binding.clock.text = timeFormat.format(date)
                binding.date.text = dateFormat.format(date)
            } else {
                binding.clock.text = null
                binding.date.text = null
            }
        }

    var clockSize: DateTimeViewSize = DEFAULT_DATE_TIME_VIEW_SIZE
        set(size) {
            field = size
            binding.apply {
                val clockSize = size.clockSize.toFloat()
                val dateSize = size.dateSize.toFloat()
                clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, clockSize)
                date.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSize)
                dateTimeWeatherSeparator.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSize)
                temp.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSize)
            }
        }

    var weatherUnit = DEFAULT_WEATHER_UNIT

    var weather: OpenWeatherApi.Response? = null
        set(weather) {
            field = weather
            if (weather == null) {
                binding.isWeatherShown = false
            } else {
                binding.apply {
                    isWeatherShown = true
                    temp.text = context.getString(
                        R.string.date_time_temperature_format,
                        weather.main.temp,
                        weatherUnit.symbol
                    )
                }

                Glide
                    .with(context)
                    .load(weather.weather[0].iconURL)
                    .into(binding.weatherIcon)
            }
        }

    var isWeatherShown: Boolean = DEFAULT_SHOW_WEATHER
        set(isWeatherShown) {
            field = isWeatherShown
            binding.isWeatherShown = isWeatherShown
        }

    var shouldUse24HrClock = DEFAULT_USE_24HR_CLOCK
        set(value) {
            field = value
            dateTime = Calendar.getInstance().time
        }

    var onRefreshWeatherRequested: (() -> Unit)? = null

    private val locale = Locale.getDefault()

    private val timeFormat
        get() = SimpleDateFormat(
            if (shouldUse24HrClock)
                "HH:mm"
            else "h:mm a",
            locale
        )

    private val dateFormat = SimpleDateFormat("MMM d", locale)

    private val binding: DateTimeViewBinding

    private val separator: TextView

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
            1f
        )
        gravity = Gravity.CENTER
        orientation = VERTICAL
        binding = DateTimeViewBinding.inflate(LayoutInflater.from(context), this, true)
        separator = binding.dateTimeWeatherSeparator
        clockSize = DateTimeViewSize.NORMAL

        applyTextShadow()

        binding.refreshWeatherBtn.setOnClickListener {
            (it as IconButton).disabled = true
            onRefreshWeatherRequested?.let { it() }
        }
    }

    private fun applyTextShadow() {
        val textColor = binding.clock.currentTextColor
        val shadowColor = Color.argb(
            resources.getInteger(R.integer.text_shadow_opacity_date_time_view),
            255 - Color.red(textColor),
            255 - Color.green(textColor),
            255 - Color.blue(textColor),
        )

        with(binding) {
            val radius =
                resources.getInteger(R.integer.text_shadow_radius_date_time_view).toFloat()
            val dx = resources.getInteger(R.integer.text_shadow_dx_date_time_view).toFloat()
            val dy = resources.getInteger(R.integer.text_shadow_dy_date_time_view).toFloat()

            clock.setShadowLayer(radius, dx, dy, shadowColor)
            date.setShadowLayer(radius, dx, dy, shadowColor)
            dateTimeWeatherSeparator.setShadowLayer(radius, dx, dy, shadowColor)
            temp.setShadowLayer(radius, dx, dy, shadowColor)
        }
    }
}
