package kenneth.app.starlightlauncher.searching.views

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.content.ContextCompat.startActivity
import kenneth.app.starlightlauncher.databinding.BluetoothControlBinding

class BluetoothControl(context: Context) : LinearLayout(context) {
    private val binding: BluetoothControlBinding

    private val bluetoothSettingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            displayBluetoothState(bluetoothAdapter.isEnabled)
        }
    }

    init {
        gravity = Gravity.CENTER_HORIZONTAL
        orientation = VERTICAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT,
        )

        binding = BluetoothControlBinding.inflate(LayoutInflater.from(context), this)

        context.registerReceiver(
            broadcastReceiver,
            IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
        )

        with(binding) {
            openBluetoothSettingsButton.setOnClickListener { openBluetoothSettings() }
            toggleBluetoothContainer.setOnClickListener { toggleBluetooth() }

            toggleBluetoothSwitch.apply {
                isChecked = bluetoothAdapter.isEnabled
                setOnClickListener { toggleBluetooth() }
            }
        }
    }

    private fun toggleBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
        } else {
            bluetoothAdapter.enable()
        }
    }

    private fun openBluetoothSettings() {
        startActivity(context, bluetoothSettingsIntent, null)
    }

    private fun displayBluetoothState(isEnabled: Boolean) {
        binding.toggleBluetoothSwitch.isChecked = isEnabled
    }
}