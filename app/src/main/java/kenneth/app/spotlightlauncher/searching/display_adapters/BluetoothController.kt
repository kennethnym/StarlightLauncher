package kenneth.app.spotlightlauncher.searching.display_adapters

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import androidx.annotation.StyleableRes
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kenneth.app.spotlightlauncher.MainActivity
import kenneth.app.spotlightlauncher.R
import kenneth.app.spotlightlauncher.utils.RecyclerViewDataAdapter

/**
 * An extension method on BluetoothDevice,
 * which returns the icon drawable representing the type of this BluetoothDevice
 */
private fun BluetoothDevice.getDeviceIcon(context: Context): Drawable =
    when (bluetoothClass.deviceClass) {
        BluetoothClass.Device.AUDIO_VIDEO_CAMCORDER,
        BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO,
        BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO,
        BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO,
        BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
        BluetoothClass.Device.AUDIO_VIDEO_SET_TOP_BOX,
        BluetoothClass.Device.AUDIO_VIDEO_UNCATEGORIZED -> {
            context.getDrawable(R.drawable.ic_twotone_speaker_24)!!
        }

        BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
        BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE -> {
            context.getDrawable(R.drawable.ic_twotone_headset_24)!!
        }

        BluetoothClass.Device.COMPUTER_DESKTOP,
        BluetoothClass.Device.COMPUTER_LAPTOP,
        BluetoothClass.Device.COMPUTER_SERVER,
        BluetoothClass.Device.COMPUTER_UNCATEGORIZED -> {
            context.getDrawable(R.drawable.ic_twotone_computer_24)!!
        }

        BluetoothClass.Device.COMPUTER_WEARABLE,
        BluetoothClass.Device.WEARABLE_GLASSES,
        BluetoothClass.Device.WEARABLE_WRIST_WATCH,
        BluetoothClass.Device.WEARABLE_PAGER,
        BluetoothClass.Device.WEARABLE_UNCATEGORIZED -> {
            context.getDrawable(R.drawable.ic_twotone_watch_24)!!
        }

        BluetoothClass.Device.WEARABLE_JACKET -> {
            context.getDrawable(R.drawable.ic_baseline_checkroom_24)!!
        }

        BluetoothClass.Device.PHONE_CELLULAR,
        BluetoothClass.Device.PHONE_CORDLESS,
        BluetoothClass.Device.PHONE_ISDN,
        BluetoothClass.Device.PHONE_SMART,
        BluetoothClass.Device.PHONE_MODEM_OR_GATEWAY,
        BluetoothClass.Device.PHONE_UNCATEGORIZED,
        BluetoothClass.Device.COMPUTER_HANDHELD_PC_PDA,
        BluetoothClass.Device.COMPUTER_PALM_SIZE_PC_PDA -> {
            context.getDrawable(R.drawable.ic_twotone_smartphone_24)!!
        }

        BluetoothClass.Device.TOY_CONTROLLER,
        BluetoothClass.Device.TOY_GAME,
        BluetoothClass.Device.TOY_UNCATEGORIZED -> {
            context.getDrawable(R.drawable.ic_twotone_gamepad_24)!!
        }

        BluetoothClass.Device.TOY_ROBOT -> {
            context.getDrawable(R.drawable.ic_twotone_android_24)!!
        }

        BluetoothClass.Device.TOY_VEHICLE -> {
            context.getDrawable(R.drawable.ic_twotone_directions_car_24)!!
        }

        else -> {
            context.getDrawable(R.drawable.ic_twotone_device_unknown_24)!!
        }
    }

class BluetoothController(private val activity: MainActivity) : BroadcastReceiver() {
    private lateinit var bluetoothControlContainer: LinearLayout
    private lateinit var nearbyBluetoothDeviceListAdapter: NearbyBluetoothDeviceListAdapter

    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

    init {
        with(activity) {
            registerReceiver(
                this@BluetoothController,
                IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            )

            registerReceiver(
                this@BluetoothController,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null && intent != null) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    displayBluetoothState(bluetoothAdapter.isEnabled)
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val newDevice =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                    if (newDevice != null) {
                        nearbyBluetoothDeviceListAdapter
                            .displayData(nearbyBluetoothDeviceListAdapter.data + newDevice)
                    }
                }
            }
        }
    }

    fun displayBluetoothControl(parentView: LinearLayout) {
        bluetoothControlContainer = activity.findViewById(R.id.toggle_bluetooth_container)
            ?: LayoutInflater.from(activity)
                .inflate(R.layout.bluetooth_control, parentView) as LinearLayout

        with(bluetoothControlContainer) {
            findViewById<LinearLayout>(R.id.toggle_bluetooth_container)
                .setOnClickListener { toggleBluetooth() }
        }

        displayBluetoothState(bluetoothAdapter.isEnabled)
    }

    /**
     * Unregister intent receiver registered by BluetoothController
     * (ACTION_STATE_CHANGED and ACTION_FOUND)
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

    private fun displayBluetoothState(isEnabled: Boolean) {
        val bluetoothControlContainer =
            activity.findViewById<LinearLayout>(R.id.bluetooth_control_container)

        if (!::nearbyBluetoothDeviceListAdapter.isInitialized) {
            nearbyBluetoothDeviceListAdapter =
                NearbyBluetoothDeviceListAdapter.getInstance(activity)
        }

        if (bluetoothControlContainer != null) {
            bluetoothControlContainer
                .findViewById<Switch>(R.id.toggle_bluetooth_switch)
                .isChecked = isEnabled

            val toggleBluetoothContainer =
                bluetoothControlContainer.findViewById<View>(R.id.toggle_bluetooth_container)

            val bluetoothLabel =
                bluetoothControlContainer.findViewById<TextView>(R.id.bluetooth_label)

            val transparent =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    activity.getColor(android.R.color.transparent)
                else
                    activity.resources.getColor(android.R.color.transparent)

            if (isEnabled) {
                val themeAttrs = activity.theme.obtainStyledAttributes(
                    intArrayOf(R.attr.colorPrimary, R.attr.colorOnPrimary)
                )

                @StyleableRes
                var attrIndex = 0

                toggleBluetoothContainer
                    .setBackgroundColor(themeAttrs.getColor(attrIndex++, transparent))

                bluetoothLabel.setTextColor(themeAttrs.getColor(attrIndex, transparent))

                nearbyBluetoothDeviceListAdapter
                    .displayData(emptyList())

                themeAttrs.recycle()
            } else {
                val themeAttrs = activity.theme.obtainStyledAttributes(
                    intArrayOf(R.attr.colorOnSurface, R.attr.colorOnBackground)
                )

                @StyleableRes
                var attrIndex = 0

                toggleBluetoothContainer.setBackgroundColor(
                    ColorUtils.setAlphaComponent(themeAttrs.getColor(attrIndex++, transparent), 26)
                )
                bluetoothLabel.setTextColor(themeAttrs.getColor(attrIndex, transparent))
                nearbyBluetoothDeviceListAdapter.hideList()

                themeAttrs.recycle()
            }
        }
    }
}

private object NearbyBluetoothDeviceListAdapter :
    RecyclerViewDataAdapter<BluetoothDevice, NearbyBluetoothDeviceListAdapter.ViewHolder>() {
    override val layoutManager: RecyclerView.LayoutManager
        get() = LinearLayoutManager(activity)

    override val recyclerView: RecyclerView
        get() = activity.findViewById(R.id.nearby_bluetooth_device_list)

    override fun getInstance(activity: Activity) = this.apply { this.activity = activity }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(
                R.layout.nearby_bluetooth_device_list_item, parent, false
            ) as LinearLayout

        return ViewHolder(view, activity)
    }

    override fun displayData(data: List<BluetoothDevice>?) {
        val bluetoothDevices = data

        if (bluetoothDevices != null) {
            this.data = bluetoothDevices

            activity.findViewById<LinearLayout>(R.id.nearby_bluetooth_device_list_container)
                .visibility = View.VISIBLE

            if (bluetoothDevices.isNotEmpty()) {
                with(activity) {
                    findViewById<RecyclerView>(R.id.nearby_bluetooth_device_list)
                        .visibility = View.VISIBLE

                    findViewById<TextView>(R.id.no_nearby_bluetooth_devices_label)
                        .visibility = View.GONE
                }

                notifyDataSetChanged()
            } else {
                with(activity) {
                    findViewById<TextView>(R.id.no_nearby_bluetooth_devices_label)
                        .visibility = View.VISIBLE

                    findViewById<RecyclerView>(R.id.nearby_bluetooth_device_list)
                        .visibility = View.GONE
                }
            }
        }
    }

    fun hideList() {
        activity.findViewById<LinearLayout>(R.id.nearby_bluetooth_device_list_container)
            .visibility = View.GONE
    }

    private class ViewHolder(view: LinearLayout, activity: Activity) :
        RecyclerViewDataAdapter.ViewHolder<BluetoothDevice>(view, activity) {
        private lateinit var bluetoothDevice: BluetoothDevice

        override fun bindWith(data: BluetoothDevice) {
            val bluetoothDevice = data

            this.bluetoothDevice = bluetoothDevice

            with(view) {
                findViewById<ImageView>(R.id.active_device_type_icon)
                    .setImageDrawable(bluetoothDevice.getDeviceIcon(activity))

                findViewById<TextView>(R.id.active_device_name)
                    .text = bluetoothDevice.name
            }
        }
    }
}
