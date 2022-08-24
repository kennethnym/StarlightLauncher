package kenneth.app.starlightlauncher.views

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.IO_DISPATCHER
import kenneth.app.starlightlauncher.MAIN_DISPATCHER
import kenneth.app.starlightlauncher.R
import kenneth.app.starlightlauncher.api.LatLong
import kenneth.app.starlightlauncher.api.OpenWeatherApi
import kenneth.app.starlightlauncher.databinding.DateTimeViewBinding
import kenneth.app.starlightlauncher.prefs.datetime.DateTimePreferenceManager
import kenneth.app.starlightlauncher.api.util.activity
import kenneth.app.starlightlauncher.api.view.IconButton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Named

@AndroidEntryPoint
internal class DateTimeView(context: Context, attrs: AttributeSet) :
    LinearLayout(context, attrs), LifecycleEventObserver,
    SharedPreferences.OnSharedPreferenceChangeListener {
    @Inject
    lateinit var locale: Locale

    @Inject
    lateinit var dateTimePreferenceManager: DateTimePreferenceManager

    @Inject
    lateinit var openWeatherApi: OpenWeatherApi

    @Inject
    lateinit var locationManager: LocationManager

    @Inject
    lateinit var sharedPreferences: SharedPreferences

    @Inject
    @Named(MAIN_DISPATCHER)
    lateinit var mainDispatcher: CoroutineDispatcher

    @Inject
    @Named(IO_DISPATCHER)
    lateinit var ioDispatcher: CoroutineDispatcher

    private val timeTickIntentFilter = IntentFilter(Intent.ACTION_TIME_TICK)

    private val weatherLocationCriteria = Criteria().apply {
        accuracy = Criteria.ACCURACY_COARSE
        powerRequirement = Criteria.POWER_LOW
    }

    /**
     * A BroadcastReceiver that receives broadcast of Intent.ACTION_TIME_TICK.
     * Must register this receiver in activity, or the time will not update.
     */
    private val timeTickBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent?.action == Intent.ACTION_TIME_TICK) {
                updateTime()
            }
        }
    }

    private val timeFormat
        get() = SimpleDateFormat(
            if (dateTimePreferenceManager.shouldUse24HrClock)
                "HH:mm"
            else "h:mm a",
            locale
        )

    private val dateFormat = SimpleDateFormat("MMM d", locale)

    private val binding: DateTimeViewBinding

    private val separator: TextView

    private var currentWeatherLocation: LatLong

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
        currentWeatherLocation = dateTimePreferenceManager.weatherLocation

        updateTime()
        applyTextShadow()
        applyClockSize()
        if (dateTimePreferenceManager.shouldShowWeather) {
            showWeather()
        }
        activity?.lifecycle?.addObserver(this)
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)

        binding.refreshWeatherBtn.setOnClickListener {
            (it as IconButton).disabled = true
            showWeather()
        }
    }

    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        if (source is AppCompatActivity) {
            when (event) {
                Lifecycle.Event.ON_RESUME -> onResume()
                Lifecycle.Event.ON_PAUSE -> onPause()
                else -> {
                }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterTimeTickListener()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerTimeTickListener()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when (key) {
            dateTimePreferenceManager.keys.useAutoWeatherLocation -> {
                toggleLocationUpdate()
            }
            dateTimePreferenceManager.keys.showWeather -> {
                val shouldShowWeather = dateTimePreferenceManager.shouldShowWeather
                binding.isWeatherShown = shouldShowWeather
                if (shouldShowWeather) {
                    showWeather()
                }
            }
            dateTimePreferenceManager.keys.autoWeatherLocationCheckFrequency -> {
                changeLocationUpdateFrequency()
            }
            dateTimePreferenceManager.keys.use24HrClock -> {
                updateTime()
            }
            dateTimePreferenceManager.keys.clockSize -> {
                applyClockSize()
            }
            dateTimePreferenceManager.keys.weatherLocationLat,
            dateTimePreferenceManager.keys.weatherLocationLong -> {
                currentWeatherLocation = dateTimePreferenceManager.weatherLocation
                showWeather()
            }
            dateTimePreferenceManager.keys.weatherUnit -> {
                showWeather()
            }
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

    private fun applyClockSize() {
        val clockSize = dateTimePreferenceManager.dateTimeViewSize.clockSize.toFloat()
        val dateSize = dateTimePreferenceManager.dateTimeViewSize.dateSize.toFloat()
        binding.apply {
            clock.setTextSize(TypedValue.COMPLEX_UNIT_SP, clockSize)
            date.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSize)
            dateTimeWeatherSeparator.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSize)
            temp.setTextSize(TypedValue.COMPLEX_UNIT_SP, dateSize)
            refreshWeatherBtn
        }
    }

    private fun onResume() {
        registerTimeTickListener()
        updateTime()
    }

    private fun onPause() {
        unregisterTimeTickListener()
    }

    private fun registerTimeTickListener() {
        context.registerReceiver(timeTickBroadcastReceiver, timeTickIntentFilter)
    }

    private fun unregisterTimeTickListener() {
        try {
            context.unregisterReceiver(timeTickBroadcastReceiver)
        } catch (ex: IllegalArgumentException) {
        }
    }

    private fun showWeather() {
        if (dateTimePreferenceManager.shouldUseAutoWeatherLocation && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.getBestProvider(weatherLocationCriteria, true)?.let {
                locationManager.getLastKnownLocation(it)?.let { loc ->
                    currentWeatherLocation = LatLong(loc)
                    loadWeather()
                }
            }
        } else {
            loadWeather()
        }
    }

    private fun loadWeather() {
        CoroutineScope(mainDispatcher).launch {
            val weatherRequestResult = try {
                openWeatherApi.run {
                    latLong = currentWeatherLocation
                    unit = dateTimePreferenceManager.weatherUnit
                    withContext(ioDispatcher) {
                        getCurrentWeather()
                    }
                }
            } catch (ex: Exception) {
                Log.e("Starlight", "$ex")
                return@launch
            }

            weatherRequestResult.getOrNull()?.let { weather ->
                binding.temp.text = context.getString(
                    R.string.date_time_temperature_format,
                    weather.main.temp,
                    openWeatherApi.unit.symbol
                )

                Glide
                    .with(context)
                    .load(weather.weather[0].iconURL)
                    .into(binding.weatherIcon)

                binding.apply {
                    isWeatherShown = true
                    weatherIcon.contentDescription = weather.weather[0].description
                    if (refreshWeatherBtn.disabled) {
                        // this loadWeather call is triggered
                        // by the refresh btn
                        // re-enable the button and tell user weather is updated
                        refreshWeatherBtn.disabled = false
                        Toast.makeText(
                            context,
                            R.string.weather_updated_message,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }
            } ?: kotlin.run {
                binding.isWeatherShown = false
            }
        }
    }

    /**
     * Stops receiving location updates from the system.
     */
    private fun stopLocationUpdates() {
        locationManager.removeUpdates(::onLocationUpdated)
    }

    private fun changeLocationUpdateFrequency() {
        stopLocationUpdates()
        locationManager.getBestProvider(weatherLocationCriteria, true)
            ?.let {
                locationManager.getLastKnownLocation(it)?.let { loc ->
                    currentWeatherLocation = LatLong(loc)
                }
                loadWeather()
                locationManager.requestLocationUpdates(
                    it,
                    dateTimePreferenceManager.autoWeatherLocationCheckFrequency,
                    1000F,
                    ::onLocationUpdated
                )
            }
    }

    /**
     * Enables or disables auto update of weather location.
     */
    private fun toggleLocationUpdate() {
        if (dateTimePreferenceManager.shouldUseAutoWeatherLocation && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            locationManager.getBestProvider(weatherLocationCriteria, true)
                ?.let {
                    locationManager.getLastKnownLocation(it)?.let { loc ->
                        currentWeatherLocation = LatLong(loc)
                    }
                    loadWeather()
                    locationManager.requestLocationUpdates(
                        it,
                        dateTimePreferenceManager.autoWeatherLocationCheckFrequency,
                        1000F,
                        ::onLocationUpdated
                    )
                }
        } else {
            currentWeatherLocation = dateTimePreferenceManager.weatherLocation
            stopLocationUpdates()
        }
    }

    private fun onLocationUpdated(location: Location) {
        currentWeatherLocation = LatLong(location)
        loadWeather()
    }

    private fun updateTime() {
        val currentTime = Calendar.getInstance().time

        binding.clock.text = timeFormat.format(currentTime)
        binding.date.text = dateFormat.format(currentTime)
    }
}
