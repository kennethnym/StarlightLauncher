package kenneth.app.spotlightlauncher.searching.display_adapters

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R

class WifiController(private val activity: MainActivity) : BroadcastReceiver() {
    private lateinit var parentView: LinearLayout
    private lateinit var wifiLabel: TextView
    private lateinit var wifiSwitch: Switch

    private val requestPermissionLauncher: ActivityResultLauncher<String>
    private val wifiManager =
        activity.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val ssid: String
        get() =
            if (wifiManager.isWifiEnabled && wifiManager.connectionInfo.supplicantState == SupplicantState.COMPLETED)
                wifiManager.connectionInfo.ssid
            else if (wifiManager.isWifiEnabled)
                activity.getString(R.string.unknown_wifi_network_label)
            else
                activity.getString(R.string.wifi_not_connected)

    init {
        val intentReceiverFilter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }

        activity.registerReceiver(this, intentReceiverFilter)

        requestPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            ::handlePermissionResult
        )
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null && ::wifiLabel.isInitialized && ::wifiSwitch.isInitialized) {
            when (intent.action) {
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    wifiLabel.text = wifiManager.connectionInfo.ssid
                }
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    wifiSwitch.isChecked = wifiManager.isWifiEnabled
                    wifiLabel.text = ssid
                }
            }
        }
    }

    fun displayWifiControl(parentView: LinearLayout) {
        this.parentView = parentView

        wifiLabel = activity.findViewById(R.id.wifi_network_name_label)
            ?: LayoutInflater.from(activity)
                .inflate(
                    R.layout.wifi_control,
                    parentView,
                ).findViewById(R.id.wifi_network_name_label)

        wifiSwitch = activity.findViewById<Switch>(R.id.wifi_switch)

        val hasCoarseLocationPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                activity.applicationContext.checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            else true

        val isWifiEnabled = wifiManager.isWifiEnabled

        with(activity) {
            wifiSwitch.apply {
                isChecked = isWifiEnabled
                setOnClickListener(::toggleWifi)
            }

            findViewById<Button>(R.id.open_wifi_settings_button)
                .setOnClickListener { openWifiSettings() }
        }

        if (hasCoarseLocationPermission) {
            wifiLabel.text = ssid
        } else {
            wifiLabel.text =
                if (isWifiEnabled) activity.getString(R.string.unknown_wifi_network_label)
                else activity.getString(R.string.wifi_not_connected)

            if (activity.findViewById<LinearLayout>(R.id.require_location_perm_notification) == null) {
                val notification = LayoutInflater.from(activity)
                    .inflate(R.layout.require_location_perm, parentView, false)
                    .also {
                        it.findViewById<Button>(R.id.grant_location_permission_button)
                            ?.setOnClickListener { askForLocationPermission() }
                    }

                parentView.addView(notification, 0)
            }
        }
    }

    /**
     * Unregister intent receiver registered by WifiController
     */
    fun unregisterIntentReceiver() {
        activity.unregisterReceiver(this)
    }

    private fun askForLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            parentView.removeView(
                activity.findViewById(R.id.require_location_perm_notification)
            )
            wifiLabel.text = ssid
        }
    }

    private fun toggleWifi(switchView: View) {
        val switch = switchView as Switch

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // in Android Q, it is not possible to toggle wifi programmatically.
            // instead, the app will bring the user to wifi settings and let them toggle wifi.

            // first, undo the check
            switch.isChecked = !switch.isChecked

            activity.startActivity(Intent(Settings.Panel.ACTION_WIFI))
        } else {
            wifiManager.isWifiEnabled = switch.isChecked
        }
    }

    private fun openWifiSettings() {
        activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
    }
}