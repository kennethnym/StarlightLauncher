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
        val intentFilter = IntentFilter().also {
            it.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            it.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        }

        activity.registerReceiver(this, intentFilter)

        requestPermissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission(),
            ::handlePermissionResult
        )
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            when (intent.action) {
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    activity.findViewById<TextView>(R.id.wifi_network_name_label)
                        ?.text = wifiManager.connectionInfo.ssid
                }
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    with(activity) {
                        findViewById<Switch>(R.id.wifi_switch)
                            ?.isChecked = wifiManager.isWifiEnabled
                        findViewById<TextView>(R.id.wifi_network_name_label)
                            ?.text = ssid
                    }
                }
            }
        }
    }

    fun displayWifiControl(parentView: LinearLayout) {
        var wifiLabel = activity.findViewById<TextView>(R.id.wifi_network_name_label)

        if (wifiLabel == null) {
            LayoutInflater.from(activity)
                .inflate(
                    R.layout.wifi_control,
                    parentView,
                )

            wifiLabel = activity.findViewById(R.id.wifi_network_name_label)
        }

        val hasCoarseLocationPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) activity.applicationContext.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED else true

        val isWifiEnabled = wifiManager.isWifiEnabled

        with(activity) {
            findViewById<Switch>(R.id.wifi_switch).apply {
                isChecked = isWifiEnabled
                setOnClickListener(::toggleWifi)
            }

            findViewById<Button>(R.id.open_wifi_settings_button)
                .setOnClickListener { openWifiSettings() }
        }

        if (hasCoarseLocationPermission) {
            wifiLabel.text = ssid

            activity.findViewById<Switch>(R.id.wifi_switch)
                .setOnClickListener(::toggleWifi)
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
                        it.findViewById<Button>(R.id.open_wifi_settings_button)
                            ?.setOnClickListener { openWifiSettings() }
                    }

                parentView.addView(notification, 0)
            }
        }
    }

    private fun askForLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            with(activity) {
                findViewById<LinearLayout>(R.id.suggested_section_card_layout)
                    .removeView(
                        activity.findViewById(R.id.require_location_perm_notification)
                    )

                findViewById<TextView>(R.id.wifi_network_name_label)
                    .text = ssid
            }
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