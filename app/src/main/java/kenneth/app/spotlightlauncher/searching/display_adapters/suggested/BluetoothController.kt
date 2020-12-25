package kenneth.app.spotlightlauncher.searching.display_adapters.suggested

import android.app.Activity
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Switch
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.views.TextButton
import javax.inject.Inject

class BluetoothController @Inject constructor(
    private val activity: Activity,
) : BroadcastReceiver() {
    private lateinit var bluetoothControlContainer: LinearLayout

    private val bluetoothSettingsIntent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    init {
        with(activity) {
            registerReceiver(
                this@BluetoothController,
                IntentFilter().apply {
                    addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                }
            )
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    displayBluetoothState(bluetoothAdapter.isEnabled)
                }
            }
        }
    }

    fun displayBluetoothControl(parentView: LinearLayout) {
        with(activity) {
            bluetoothControlContainer = findViewById(R.id.toggle_bluetooth_container)
                ?: LayoutInflater.from(activity)
                    .inflate(R.layout.bluetooth_control, parentView) as LinearLayout

            findViewById<TextButton>(R.id.open_bluetooth_settings_button)
                .setOnClickListener { openBluetoothSettings() }
        }

        with(bluetoothControlContainer) {
            findViewById<LinearLayout>(R.id.toggle_bluetooth_container)
                .setOnClickListener { toggleBluetooth() }
        }

        displayBluetoothState(bluetoothAdapter.isEnabled)
    }

    /**
     * Unregister intent receiver registered by BluetoothController
     * (ACTION_STATE_CHANGED)
     */
    fun unregisterIntentReceiver() {
        activity.unregisterReceiver(this)
    }

    private fun toggleBluetooth() {
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable()
        } else {
            bluetoothAdapter.enable()
        }
    }

    private fun openBluetoothSettings() {
        activity.startActivity(bluetoothSettingsIntent)
    }

    private fun displayBluetoothState(isEnabled: Boolean) {
        val bluetoothControlContainer =
            activity.findViewById<LinearLayout>(R.id.bluetooth_control_container)

        if (bluetoothControlContainer != null) {
            bluetoothControlContainer
                .findViewById<Switch>(R.id.toggle_bluetooth_switch)
                .isChecked = isEnabled
        }
    }
}
