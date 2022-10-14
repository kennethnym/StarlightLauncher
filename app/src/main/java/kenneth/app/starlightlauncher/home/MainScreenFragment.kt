package kenneth.app.starlightlauncher.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.starlightlauncher.BindingRegister
import kenneth.app.starlightlauncher.databinding.FragmentMainScreenBinding
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
internal class MainScreenFragment @Inject constructor(
    private val bindingRegister: BindingRegister,
) : Fragment() {
    private var binding: FragmentMainScreenBinding? = null
    private val viewModel: MainScreenViewModel by viewModels()

    /**
     * A BroadcastReceiver that receives broadcast of Intent.ACTION_TIME_TICK.
     * Must register this receiver in activity, or the time will not update.
     */
    private val timeTickBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (context != null && intent?.action == Intent.ACTION_TIME_TICK) {
                updateDateTime(Calendar.getInstance().time)
            }
        }
    }

    override fun onAttach(context: Context) {
        context.registerReceiver(timeTickBroadcastReceiver, IntentFilter(Intent.ACTION_TIME_TICK))
        super.onAttach(context)
    }

    override fun onDestroy() {
        context?.unregisterReceiver(timeTickBroadcastReceiver)
        super.onDestroy()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = context?.let {
        FragmentMainScreenBinding.inflate(inflater).run {
            binding = this
            bindingRegister.mainScreenBinding = this

            // show the current time
            dateTimeView.dateTime = Calendar.getInstance().time

            viewModel.clockSize.observe(viewLifecycleOwner) {
                dateTimeView.clockSize = it
            }

            observeWeatherInfo()

            root
        }
    }

    private fun observeWeatherInfo() {
        viewModel.weatherInfo.observe(viewLifecycleOwner) { data ->
            if (data != null) {
                val (weatherUnit, weather) = data
                binding?.dateTimeView?.apply {
                    this.weatherUnit = weatherUnit
                    this.weather = weather
                }
            } else {
                binding?.dateTimeView?.isWeatherShown = false
            }
        }
    }

    private fun updateDateTime(date: Date) {
        binding?.dateTimeView?.dateTime = date
    }
}
