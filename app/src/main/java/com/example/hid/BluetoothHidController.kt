package com.example.hid

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@SuppressLint("MissingPermission")
class BluetoothHidController(
    private val context: Context,
) {
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var hidDevice: BluetoothHidDevice? = null
    private var connectedHost: BluetoothDevice? = null
    private var activeProfile: CompatibilityProfile = CompatibilityProfile.UNIVERSAL_TV
    private var isAppRegistered = false
    private var receiverRegistered = false

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Idle)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<PairedDeviceUi>>(emptyList())
    val pairedDevices: StateFlow<List<PairedDeviceUi>> = _pairedDevices.asStateFlow()

    private val _lastReportHex = MutableStateFlow("ID:01 [08 00 00 00 00]")
    val lastReportHex: StateFlow<String> = _lastReportHex.asStateFlow()

    private val _packetCount = MutableStateFlow(0L)
    val packetCount: StateFlow<Long> = _packetCount.asStateFlow()

    var onDeviceConnectedCallback: ((String) -> Unit)? = null

    private val btReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        initialize(activeProfile)
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        isAppRegistered = false
                        connectedHost = null
                        _connectionState.value = ConnectionState.BluetoothDisabled
                        _pairedDevices.value = emptyList()
                    }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    refreshPairedDevices()
                    val device: BluetoothDevice? =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    if (bondState == BluetoothDevice.BOND_BONDED && device != null && isAppRegistered && connectedHost == null) {
                        connectToDevice(device.address)
                    }
                }
            }
        }
    }

    fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADVERTISE) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    fun requiredRuntimePermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            emptyArray()
        }
    }

    fun isBluetoothAvailable(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    fun startListening() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(btReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(btReceiver, filter)
            }
            receiverRegistered = true
        }
    }

    fun initialize(profile: CompatibilityProfile) {
        activeProfile = profile
        startListening()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            _connectionState.value = ConnectionState.UnsupportedApi(
                "Bluetooth HID Device API требует Android 9 (API 28) или выше."
            )
            return
        }

        if (bluetoothAdapter == null) {
            _connectionState.value = ConnectionState.UnsupportedApi(
                "На этом устройстве отсутствует адаптер Bluetooth."
            )
            return
        }

        if (!hasRequiredPermissions()) {
            _connectionState.value = ConnectionState.PermissionRequired
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            _connectionState.value = ConnectionState.BluetoothDisabled
            return
        }

        refreshPairedDevices()

        if (hidDevice != null) {
            registerHidApp(profile)
            return
        }

        _connectionState.value = ConnectionState.Registering
        bluetoothAdapter.getProfileProxy(
            context,
            object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profileId: Int, proxy: BluetoothProfile?) {
                    if (profileId == BluetoothProfile.HID_DEVICE && proxy is BluetoothHidDevice) {
                        hidDevice = proxy
                        registerHidApp(activeProfile)
                    }
                }

                override fun onServiceDisconnected(profileId: Int) {
                    if (profileId == BluetoothProfile.HID_DEVICE) {
                        hidDevice = null
                        isAppRegistered = false
                        connectedHost = null
                        _connectionState.value = ConnectionState.Idle
                    }
                }
            },
            BluetoothProfile.HID_DEVICE,
        )
    }

    fun switchProfile(newProfile: CompatibilityProfile) {
        if (activeProfile == newProfile && isAppRegistered) return
        activeProfile = newProfile
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && hidDevice != null && hasRequiredPermissions() && isBluetoothEnabled()) {
            try {
                if (isAppRegistered) {
                    hidDevice?.unregisterApp()
                    isAppRegistered = false
                }
            } catch (_: Exception) {
            }
            registerHidApp(newProfile)
        }
    }

    private fun registerHidApp(profile: CompatibilityProfile) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val device = hidDevice ?: return
        if (!hasRequiredPermissions()) {
            _connectionState.value = ConnectionState.PermissionRequired
            return
        }

        _connectionState.value = ConnectionState.Registering

        val sdp = BluetoothHidDeviceAppSdpSettings(
            "Dendy Pad",
            "8-Bit NES Bluetooth Gamepad",
            "Dendy",
            HidDescriptors.subclassFor(profile),
            HidDescriptors.descriptorFor(profile),
        )

        val outQos = BluetoothHidDeviceAppQosSettings(
            BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
            800,
            9,
            0,
            11250,
            BluetoothHidDeviceAppQosSettings.MAX,
        )

        val callback = object : BluetoothHidDevice.Callback() {
            override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
                isAppRegistered = registered
                if (registered) {
                    if (pluggedDevice != null && connectedHost == null) {
                        connectedHost = pluggedDevice
                        val name = safeDeviceName(pluggedDevice)
                        _connectionState.value = ConnectionState.Connected(name, pluggedDevice.address)
                        onDeviceConnectedCallback?.invoke(pluggedDevice.address)
                    } else if (connectedHost == null) {
                        _connectionState.value = ConnectionState.ReadyToPair
                    }
                    refreshPairedDevices()
                } else {
                    connectedHost = null
                    _connectionState.value = ConnectionState.Idle
                    refreshPairedDevices()
                }
            }

            override fun onConnectionStateChanged(btDevice: BluetoothDevice?, state: Int) {
                when (state) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        connectedHost = btDevice
                        val name = btDevice?.let { safeDeviceName(it) } ?: "TV"
                        val addr = btDevice?.address ?: ""
                        _connectionState.value = ConnectionState.Connected(name, addr)
                        if (addr.isNotEmpty()) {
                            onDeviceConnectedCallback?.invoke(addr)
                        }
                        refreshPairedDevices()
                    }

                    BluetoothProfile.STATE_CONNECTING -> {
                        val name = btDevice?.let { safeDeviceName(it) } ?: "TV"
                        _connectionState.value = ConnectionState.Connecting(name)
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        if (connectedHost?.address == btDevice?.address) {
                            connectedHost = null
                        }
                        _connectionState.value =
                            if (isAppRegistered) ConnectionState.ReadyToPair else ConnectionState.Idle
                        refreshPairedDevices()
                    }
                }
            }

            override fun onGetReport(
                btDevice: BluetoothDevice?,
                type: Byte,
                id: Byte,
                bufferSize: Int,
            ) {
                val reply = when (id.toInt()) {
                    HidDescriptors.REPORT_ID_GAMEPAD -> HidDescriptors.buildGamepadReport(
                        pressed = emptySet(),
                        turboOnPhase = false,
                        swapAB = false,
                        includeHatSwitch = activeProfile != CompatibilityProfile.PURE_GAMEPAD_XY,
                    )

                    HidDescriptors.REPORT_ID_KEYBOARD -> ByteArray(8)
                    HidDescriptors.REPORT_ID_CONSUMER -> ByteArray(2)
                    else -> ByteArray(1)
                }
                try {
                    device.replyReport(btDevice, type, id, reply)
                } catch (_: Exception) {
                }
            }

            override fun onSetReport(
                btDevice: BluetoothDevice?,
                type: Byte,
                id: Byte,
                data: ByteArray?,
            ) {
                try {
                    device.reportError(btDevice, BluetoothHidDevice.ERROR_RSP_SUCCESS)
                } catch (_: Exception) {
                }
            }

            override fun onSetProtocol(btDevice: BluetoothDevice?, protocol: Byte) {
                // Acknowledge protocol changes cleanly for Smart TV compatibility
            }
        }

        try {
            val started = device.registerApp(sdp, null, outQos, executor, callback)
            if (!started) {
                _connectionState.value = ConnectionState.Error(
                    "Не удалось зарегистрировать HID-профиль. Попробуйте перезапустить Bluetooth."
                )
            }
        } catch (e: SecurityException) {
            _connectionState.value = ConnectionState.PermissionRequired
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error(
                e.localizedMessage ?: "Ошибка регистрации Bluetooth HID"
            )
        }
    }

    fun refreshPairedDevices() {
        if (!hasRequiredPermissions() || bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            _pairedDevices.value = emptyList()
            return
        }
        try {
            val bonded = bluetoothAdapter.bondedDevices.orEmpty()
            val mapped = bonded.map { dev ->
                val major = dev.bluetoothClass?.majorDeviceClass ?: 0
                val isTvOrPc = major == BluetoothClass.Device.Major.AUDIO_VIDEO ||
                    major == BluetoothClass.Device.Major.COMPUTER ||
                    major == BluetoothClass.Device.Major.UNCATEGORIZED ||
                    (dev.name?.contains("TV", ignoreCase = true) == true) ||
                    (dev.name?.contains("Shield", ignoreCase = true) == true) ||
                    (dev.name?.contains("Mi Box", ignoreCase = true) == true) ||
                    (dev.name?.contains("Google", ignoreCase = true) == true) ||
                    (dev.name?.contains("LG", ignoreCase = true) == true) ||
                    (dev.name?.contains("Samsung", ignoreCase = true) == true)

                PairedDeviceUi(
                    name = safeDeviceName(dev),
                    address = dev.address,
                    isConnected = connectedHost?.address == dev.address,
                    isLikelyTvOrPc = isTvOrPc,
                )
            }.sortedWith(compareByDescending<PairedDeviceUi> { it.isConnected }.thenByDescending { it.isLikelyTvOrPc }.thenBy { it.name })
            _pairedDevices.value = mapped
        } catch (_: SecurityException) {
            _pairedDevices.value = emptyList()
        }
    }

    fun connectToDevice(address: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        if (!hasRequiredPermissions()) {
            _connectionState.value = ConnectionState.PermissionRequired
            return
        }
        val adapter = bluetoothAdapter ?: return
        val proxy = hidDevice ?: return
        try {
            val device = adapter.getRemoteDevice(address)
            _connectionState.value = ConnectionState.Connecting(safeDeviceName(device))
            proxy.connect(device)
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.Error("Не удалось подключиться: ${e.localizedMessage ?: "ошибка"}")
        }
    }

    fun disconnect() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val host = connectedHost ?: return
        try {
            hidDevice?.disconnect(host)
        } catch (_: Exception) {
        }
    }

    fun sendGamepadState(
        pressed: Set<NesButton>,
        turboOnPhase: Boolean,
        settings: GamepadSettings,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val profile = settings.compatibilityProfile

        val emitGamepad = profile == CompatibilityProfile.UNIVERSAL_TV ||
            profile == CompatibilityProfile.DUAL_HYBRID ||
            profile == CompatibilityProfile.PURE_GAMEPAD_XY

        val emitKeyboard = profile == CompatibilityProfile.DUAL_HYBRID ||
            profile == CompatibilityProfile.SMART_TV_KEYBOARD

        if (emitGamepad) {
            val gpBytes = HidDescriptors.buildGamepadReport(
                pressed = pressed,
                turboOnPhase = turboOnPhase,
                swapAB = settings.swapAB,
                includeHatSwitch = profile != CompatibilityProfile.PURE_GAMEPAD_XY,
            )
            sendRawReport(HidDescriptors.REPORT_ID_GAMEPAD, gpBytes)
        }

        if (emitKeyboard) {
            val kbBytes = HidDescriptors.buildKeyboardReport(
                pressed = pressed,
                turboOnPhase = turboOnPhase,
                swapAB = settings.swapAB,
                preset = settings.keyboardPreset,
            )
            sendRawReport(HidDescriptors.REPORT_ID_KEYBOARD, kbBytes)
        }
    }

    fun sendTvAction(action: TvAction) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val usage = HidDescriptors.consumerUsageFor(action)
        val downBytes = byteArrayOf(
            (usage and 0xFF).toByte(),
            ((usage shr 8) and 0xFF).toByte(),
        )
        sendRawReport(HidDescriptors.REPORT_ID_CONSUMER, downBytes)
        mainHandler.postDelayed({
            sendRawReport(HidDescriptors.REPORT_ID_CONSUMER, byteArrayOf(0x00, 0x00))
        }, 45L)
    }

    private fun sendRawReport(reportId: Int, data: ByteArray) {
        _lastReportHex.value = HidDescriptors.formatHex(reportId, data)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val host = connectedHost ?: return
        val device = hidDevice ?: return
        executor.execute {
            try {
                if (device.sendReport(host, reportId, data)) {
                    _packetCount.value = _packetCount.value + 1L
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun safeDeviceName(device: BluetoothDevice): String {
        return try {
            device.name?.takeIf { it.isNotBlank() } ?: device.address
        } catch (_: SecurityException) {
            device.address
        }
    }

    fun release() {
        try {
            if (receiverRegistered) {
                context.unregisterReceiver(btReceiver)
                receiverRegistered = false
            }
        } catch (_: Exception) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                if (isAppRegistered) {
                    hidDevice?.unregisterApp()
                }
                hidDevice?.let { bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) }
            } catch (_: Exception) {
            }
        }
        hidDevice = null
        connectedHost = null
        isAppRegistered = false
    }
}
