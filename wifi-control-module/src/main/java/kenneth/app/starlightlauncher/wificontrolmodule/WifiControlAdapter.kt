package kenneth.app.starlightlauncher.wificontrolmodule

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.SupplicantState
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import kenneth.app.starlightlauncher.api.SearchResult
import kenneth.app.starlightlauncher.api.StarlightLauncherApi
import kenneth.app.starlightlauncher.api.view.SearchResultAdapter
import kenneth.app.starlightlauncher.wificontrolmodule.databinding.WifiControlBinding

class WifiControlAdapter(
    private val context: Context,
    private val launcher: StarlightLauncherApi,
) : SearchResultAdapter {
    private val connectivityManager =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    private val wifiManager =
        context.applicationContext.getSystemService(WifiManager::class.java)

    private var binding: WifiControlBinding? = null

    private var currentWifiSsid: String? = null

    private var isWifiEnabled = false

    private val networkStateListener = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            isWifiEnabled = false

            val hasFineLocationPerm =
                context.applicationContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

            val hasNetworkStatePerm =
                context.applicationContext.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED

            binding?.let { showWifiStatus(it, hasNetworkStatePerm, hasFineLocationPerm) }
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            isWifiEnabled = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

            val hasFineLocationPerm =
                context.applicationContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

            val hasNetworkStatePerm =
                context.applicationContext.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED

            val transportInfo =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    networkCapabilities.transportInfo
                else null

            if (hasFineLocationPerm) {
                currentWifiSsid = when {
                    transportInfo != null && transportInfo is WifiInfo ->
                        transportInfo.ssid

                    wifiManager.connectionInfo.supplicantState == SupplicantState.COMPLETED ->
                        wifiManager.connectionInfo.ssid

                    else -> context.getString(R.string.unknown_wifi_network_label)
                }
            }

            binding?.let { showWifiStatus(it, hasNetworkStatePerm, hasFineLocationPerm) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup): SearchResultAdapter.ViewHolder {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        // requires ACCESS_NETWORK_STATE permission
        connectivityManager.registerNetworkCallback(networkRequest, networkStateListener)

        val binding = WifiControlBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return WifiControlViewHolder(binding)
    }

    override fun onBindSearchResult(
        holder: SearchResultAdapter.ViewHolder,
        searchResult: SearchResult
    ) {
        if (holder is WifiControlViewHolder && searchResult is WifiControlModule.ShowControl) {
            onBindSearchResult(holder)
        }
    }

    private fun onBindSearchResult(holder: WifiControlViewHolder) {
        binding = holder.binding

        holder.binding.wifiControlBg.blurWith(launcher.blurHandler)

        val hasFineLocationPerm =
            context.applicationContext.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

        val hasNetworkStatePerm =
            context.applicationContext.checkSelfPermission(Manifest.permission.ACCESS_NETWORK_STATE) == PackageManager.PERMISSION_GRANTED

        showWifiStatus(holder.binding, hasNetworkStatePerm, hasFineLocationPerm)

        holder.binding.openWifiSettingsButton.setOnClickListener { openWifiSettings() }
    }

    private fun showWifiStatus(
        binding: WifiControlBinding,
        hasNetworkStatePermission: Boolean,
        hasFineLocationPermission: Boolean,
    ) {
        if (hasNetworkStatePermission || hasFineLocationPermission) {
            binding.apply {
                hasAllPermissions = hasFineLocationPermission && hasNetworkStatePermission
                this.hasNetworkStatePermission = hasNetworkStatePermission
                hasLocationPermission = hasFineLocationPermission
                isWifiEnabled = this@WifiControlAdapter.isWifiEnabled
                wifiSsid = when {
                    this@WifiControlAdapter.isWifiEnabled -> currentWifiSsid
                        ?: context.getString(R.string.unknown_wifi_network_label)
                    else -> context.getString(R.string.wifi_not_connected)
                }
            }
        } else {
            binding.apply {
                hasAllPermissions = false
                this.hasNetworkStatePermission = hasNetworkStatePermission
                hasLocationPermission = hasFineLocationPermission
                if (hasNetworkStatePermission) {
                    isWifiEnabled = this@WifiControlAdapter.isWifiEnabled
                }
            }
        }

        with(binding) {
            grantPermissionsButton.setOnClickListener {
                requestRequiredPermissions()
            }
            wifiSwitch.setOnClickListener { toggleWifi() }
        }
    }

    private fun requestRequiredPermissions() {
        launcher.requestPermissions(
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) { results ->
            val hasNetworkStatePermission =
                results[Manifest.permission.ACCESS_NETWORK_STATE] ?: return@requestPermissions
            val hasFineLocationPermission =
                results[Manifest.permission.ACCESS_FINE_LOCATION] ?: return@requestPermissions

            binding?.let {
                showWifiStatus(it, hasNetworkStatePermission, hasFineLocationPermission)
            }
        }
    }

    private fun toggleWifi() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding?.apply {
                // first, undo the check
                wifiSwitch.isChecked = !wifiSwitch.isChecked
            }

            // in Android Q, it is not possible to toggle wifi programmatically.
            // instead, the app will bring the user to wifi settings and let them toggle wifi.
            openWifiSettings()
        } else {
            binding?.let {
                wifiManager.isWifiEnabled = it.wifiSwitch.isChecked
            }
        }
    }

    private fun openWifiSettings() {
        startActivity(
            context,
            Intent(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    Settings.Panel.ACTION_WIFI
                else
                    Settings.ACTION_WIFI_SETTINGS
            ),
            null
        )
    }
}

class WifiControlViewHolder(internal val binding: WifiControlBinding) :
    SearchResultAdapter.ViewHolder {
    override val rootView = binding.root
}