package kenneth.app.spotlightlauncher.searching.views

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import kenneth.app.spotlightlauncher.PermissionHandler
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.databinding.WifiControlBinding
import javax.inject.Inject

@AndroidEntryPoint
class WifiControl(context: Context) : LinearLayout(context) {
    @Inject
    lateinit var wifiManager: WifiManager

    @Inject
    lateinit var permissionHandler: PermissionHandler

    private val binding: WifiControlBinding

    private val wifiSsid: String
        get() =
            if (wifiManager.isWifiEnabled && wifiManager.connectionInfo.supplicantState == SupplicantState.COMPLETED)
                wifiManager.connectionInfo.ssid
            else if (wifiManager.isWifiEnabled)
                context.getString(R.string.unknown_wifi_network_label)
            else
                context.getString(R.string.wifi_not_connected)

    init {
        gravity = Gravity.CENTER
        orientation = VERTICAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        binding = WifiControlBinding.inflate(LayoutInflater.from(context), this)

        showWifiStatus()

        binding.wifiSwitch.setOnClickListener(::toggleWifi)
    }

    private fun showWifiStatus() {
        val hasCoarseLocationPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                context.applicationContext.checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            else true

        if (hasCoarseLocationPermission) {
            binding.wifiNetworkNameLabel.text = wifiSsid
        } else {
            showLocationRequiredNotification()
        }
    }

    private fun showLocationRequiredNotification() {
        binding.wifiNetworkNameLabel.text =
            if (wifiManager.isWifiEnabled) context.getString(R.string.unknown_wifi_network_label)
            else context.getString(R.string.wifi_not_connected)

        with(binding.locationPermRequiredNotification) {
            locationPermNotificationContainer.isVisible = true
            grantLocationPermissionButton.setOnClickListener { askForLocationPermission() }
        }
    }

    private fun askForLocationPermission() {
        permissionHandler.run {
            addListener(Manifest.permission.ACCESS_COARSE_LOCATION, ::handlePermissionResult)
            requestPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    private fun handlePermissionResult(isGranted: Boolean) {
        if (isGranted) {
            binding.locationPermRequiredNotification.locationPermNotificationContainer
                .isVisible = false

            binding.wifiNetworkNameLabel.text = wifiSsid
        }
    }

    private fun toggleWifi(switchView: View) {
        val switch = switchView as SwitchCompat

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // in Android Q, it is not possible to toggle wifi programmatically.
            // instead, the app will bring the user to wifi settings and let them toggle wifi.

            // first, undo the check
            switch.isChecked = !switch.isChecked

            startActivity(context, Intent(Settings.Panel.ACTION_WIFI), null)
        } else {
            wifiManager.isWifiEnabled = switch.isChecked
        }
    }
}