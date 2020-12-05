package kenneth.app.spotlightlauncher.views

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.R
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class DateTimeView(context: Context, attrs: AttributeSet) : LinearLayout(context, attrs) {
    @Inject
    lateinit var locale: Locale

    @Inject
    lateinit var calendar: Calendar

    private val timeTickIntentFilter = IntentFilter(Intent.ACTION_TIME_TICK)

    /**
     * A BroadcastReceiver that receives broadcast of Intent.ACTION_TIME_TICK.
     * Must register this receiver in activity, or the time will not update.
     */
    private val timeTickBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent != null) {
                if (intent.action == Intent.ACTION_TIME_TICK) {
                    updateTime()
                }
            }
        }
    }

    var layoutWeight: Float
        get() = (layoutParams as LayoutParams).weight
        set(newWeight) {
            val newLayoutParams = (layoutParams as LayoutParams).apply {
                weight = newWeight
            }
            layoutParams = newLayoutParams
        }

    private val timeFormat = SimpleDateFormat("K:mm", locale)
    private val dateFormat = SimpleDateFormat("MMM d", locale)

    private val clock: TextView
    private val date: TextView

    init {
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
            1f
        )
        gravity = Gravity.CENTER
        orientation = VERTICAL

        inflate(context, R.layout.date_time_view, this)

        clock = findViewById(R.id.clock)
        date = findViewById(R.id.date)

        updateTime()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)

        if (visibility == View.GONE) {
            unregisterTickTickListener()
        } else {
            registerTimeTickListener()
        }
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)

        if (hasWindowFocus) {
            registerTimeTickListener()
        } else {
            unregisterTickTickListener()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterTickTickListener()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerTimeTickListener()
    }

    private fun registerTimeTickListener() {
        context.registerReceiver(timeTickBroadcastReceiver, timeTickIntentFilter)
    }

    private fun unregisterTickTickListener() {
        context.unregisterReceiver(timeTickBroadcastReceiver)
    }

    private fun updateTime() {
        val currentTime = calendar.time

        clock.text = timeFormat.format(currentTime)
        date.text = dateFormat.format(currentTime)
    }
}
