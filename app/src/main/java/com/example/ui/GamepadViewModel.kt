package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.RetroSoundEngine
import com.example.data.SettingsRepository
import com.example.hid.BluetoothHidController
import com.example.hid.CompatibilityProfile
import com.example.hid.ConnectionState
import com.example.hid.DendySkin
import com.example.hid.DiscoveredTvUi
import com.example.hid.GamepadSettings
import com.example.hid.KeyboardKeyPreset
import com.example.hid.NesButton
import com.example.hid.PairedDeviceUi
import com.example.hid.TransportMode
import com.example.hid.TurboRate
import com.example.hid.TvAction
import com.example.hid.WifiConnectionState
import com.example.hid.WifiProtocol
import com.example.wifi.WifiGamepadController
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GamepadViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepo = SettingsRepository(application)
    val hidController = BluetoothHidController(application)
    val wifiController = WifiGamepadController(application)
    private val soundEngine = RetroSoundEngine()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    val settings: StateFlow<GamepadSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GamepadSettings(),
    )

    val connectionState: StateFlow<ConnectionState> = hidController.connectionState
    val pairedDevices: StateFlow<List<PairedDeviceUi>> = hidController.pairedDevices
    val lastReportHex: StateFlow<String> = hidController.lastReportHex
    val btPacketCount: StateFlow<Long> = hidController.packetCount

    val wifiState: StateFlow<WifiConnectionState> = wifiController.wifiState
    val discoveredTvs: StateFlow<List<DiscoveredTvUi>> = wifiController.discoveredTvs
    val isWifiScanning: StateFlow<Boolean> = wifiController.isScanning
    val localPhoneIp: StateFlow<String> = wifiController.localPhoneIp
    val webClientsCount: StateFlow<Int> = wifiController.webClientsCount
    val lastWifiPacket: StateFlow<String> = wifiController.lastWifiPacket
    val wifiPacketCount: StateFlow<Long> = wifiController.packetCount

    private val _pressedButtons = MutableStateFlow<Set<NesButton>>(emptySet())
    val pressedButtons: StateFlow<Set<NesButton>> = _pressedButtons.asStateFlow()

    private val _turboPhaseOn = MutableStateFlow(false)
    val turboPhaseOn: StateFlow<Boolean> = _turboPhaseOn.asStateFlow()

    private var turboJob: Job? = null

    init {
        hidController.onDeviceConnectedCallback = { address ->
            viewModelScope.launch {
                settingsRepo.update { it.copy(lastConnectedAddress = address) }
            }
        }
        viewModelScope.launch {
            val initial = settingsRepo.settingsFlow.first()
            hidController.initialize(initial.compatibilityProfile)
            wifiController.refreshLocalNetwork()
        }
    }

    fun onPermissionsOrBtChanged() {
        hidController.initialize(settings.value.compatibilityProfile)
        wifiController.refreshLocalNetwork()
    }

    fun refreshDevices() {
        hidController.refreshPairedDevices()
        wifiController.refreshLocalNetwork()
    }

    fun connectToDevice(address: String) {
        hidController.connectToDevice(address)
    }

    fun disconnectDevice() {
        clearAllButtons()
        hidController.disconnect()
    }

    // --- Wi-Fi controls ---
    fun startWifiTvScan() {
        wifiController.startTvScan()
    }

    fun stopWifiTvScan() {
        wifiController.stopTvScan()
    }

    fun selectDiscoveredTv(tv: DiscoveredTvUi) {
        viewModelScope.launch {
            val current = settings.value
            when {
                tv.isAdbPairing -> {
                    val updated = current.copy(
                        transportMode = TransportMode.WIFI,
                        wifiProtocol = WifiProtocol.ADB_PAIRING_CODE,
                        wifiTargetIp = tv.ip,
                        wifiPairPort = tv.port,
                    )
                    settingsRepo.update { updated }
                }

                tv.isAdbConnect -> {
                    val updated = current.copy(
                        transportMode = TransportMode.WIFI,
                        wifiProtocol = WifiProtocol.ADB_PAIRING_CODE,
                        wifiTargetIp = tv.ip,
                        wifiConnectPort = tv.port,
                    )
                    settingsRepo.update { updated }
                    wifiController.connect(updated)
                }

                else -> {
                    val updated = current.copy(
                        transportMode = TransportMode.WIFI,
                        wifiTargetIp = tv.ip,
                    )
                    settingsRepo.update { updated }
                    wifiController.connect(updated)
                }
            }
        }
    }

    fun pairWithWifiCode(
        ip: String,
        pairPort: Int,
        pairingCode: String,
        connectPort: Int?,
    ) {
        viewModelScope.launch {
            val current = settings.value
            val updated = current.copy(
                transportMode = TransportMode.WIFI,
                wifiProtocol = WifiProtocol.ADB_PAIRING_CODE,
                wifiTargetIp = ip.trim(),
                wifiPairPort = pairPort,
                wifiConnectPort = connectPort ?: current.wifiConnectPort,
            )
            settingsRepo.update { updated }
            wifiController.pairWithCodeAndConnect(
                ip = ip.trim(),
                pairPort = pairPort,
                pairingCode = pairingCode.trim(),
                connectPort = connectPort,
            )
        }
    }

    fun connectWifi(ip: String? = null, port: Int? = null, connectPort: Int? = null) {
        viewModelScope.launch {
            val current = settings.value
            val nextIp = ip?.trim() ?: current.wifiTargetIp
            val nextPort = port ?: current.wifiPort
            val nextConnectPort = connectPort ?: current.wifiConnectPort
            val updated = current.copy(
                transportMode = TransportMode.WIFI,
                wifiTargetIp = nextIp,
                wifiPort = nextPort,
                wifiConnectPort = nextConnectPort,
            )
            settingsRepo.update { updated }
            wifiController.connect(updated)
        }
    }

    fun disconnectWifi() {
        clearAllButtons()
        wifiController.disconnect()
    }

    fun setTransportMode(mode: TransportMode) {
        clearAllButtons()
        viewModelScope.launch {
            settingsRepo.update { it.copy(transportMode = mode) }
            if (mode == TransportMode.WIFI) {
                wifiController.refreshLocalNetwork()
            }
        }
    }

    fun setWifiProtocol(protocol: WifiProtocol) {
        clearAllButtons()
        wifiController.disconnect()
        viewModelScope.launch {
            settingsRepo.update {
                it.copy(
                    wifiProtocol = protocol,
                    wifiPort = protocol.defaultPort,
                )
            }
        }
    }

    fun setWifiTargetIp(ip: String) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(wifiTargetIp = ip) }
        }
    }

    fun setWifiPort(port: Int) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(wifiPort = port.coerceIn(1, 65535)) }
        }
    }

    fun setWifiPlayerIndex(index: Int) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(wifiPlayerIndex = index.coerceIn(0, 3)) }
        }
    }

    // --- Input dispatch ---
    fun updateButtons(nextButtons: Set<NesButton>) {
        val previous = _pressedButtons.value
        if (nextButtons == previous) return

        val newlyPressed = nextButtons - previous
        _pressedButtons.value = nextButtons

        if (newlyPressed.isNotEmpty()) {
            triggerFeedback(newlyPressed)
        }

        val hasTurbo = NesButton.TURBO_A in nextButtons || NesButton.TURBO_B in nextButtons
        if (hasTurbo && turboJob == null) {
            startTurboLoop()
        } else if (!hasTurbo && turboJob != null) {
            stopTurboLoop()
        }

        dispatchGamepadState(nextButtons, _turboPhaseOn.value)
    }

    fun clearAllButtons() {
        stopTurboLoop()
        if (_pressedButtons.value.isNotEmpty()) {
            _pressedButtons.value = emptySet()
            dispatchGamepadState(emptySet(), false)
        }
    }

    private fun dispatchGamepadState(pressed: Set<NesButton>, turboOn: Boolean) {
        val cfg = settings.value
        when (cfg.transportMode) {
            TransportMode.BLUETOOTH_HID -> hidController.sendGamepadState(pressed, turboOn, cfg)
            TransportMode.WIFI -> wifiController.sendGamepadState(pressed, turboOn, cfg)
        }
    }

    fun sendTvAction(action: TvAction) {
        val cfg = settings.value
        if (cfg.hapticEnabled) performTickVibration()
        if (cfg.soundEnabled) soundEngine.playSystemClick()
        when (cfg.transportMode) {
            TransportMode.BLUETOOTH_HID -> hidController.sendTvAction(action)
            TransportMode.WIFI -> wifiController.sendTvAction(action, cfg)
        }
    }

    private fun startTurboLoop() {
        turboJob?.cancel()
        turboJob = viewModelScope.launch {
            _turboPhaseOn.value = true
            dispatchGamepadState(_pressedButtons.value, true)
            while (isActive) {
                val halfPeriodMs = (500L / settings.value.turboRate.hz).coerceAtLeast(15L)
                delay(halfPeriodMs)
                val nextPhase = !_turboPhaseOn.value
                _turboPhaseOn.value = nextPhase
                dispatchGamepadState(_pressedButtons.value, nextPhase)
            }
        }
    }

    private fun stopTurboLoop() {
        turboJob?.cancel()
        turboJob = null
        _turboPhaseOn.value = false
    }

    private fun triggerFeedback(newlyPressed: Set<NesButton>) {
        val cfg = settings.value
        if (cfg.hapticEnabled) {
            performTickVibration()
        }
        if (cfg.soundEnabled) {
            when {
                newlyPressed.any { it == NesButton.A || it == NesButton.B || it == NesButton.TURBO_A || it == NesButton.TURBO_B } ->
                    soundEngine.playActionClick()
                newlyPressed.any { it == NesButton.SELECT || it == NesButton.START } ->
                    soundEngine.playSystemClick()
                else -> soundEngine.playDpadClick()
            }
        }
    }

    private fun performTickVibration() {
        val vib = vibrator ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vib.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vib.vibrate(VibrationEffect.createOneShot(14L, 170))
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(14L)
            }
        } catch (_: Exception) {
        }
    }

    fun setCompatibilityProfile(profile: CompatibilityProfile) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(compatibilityProfile = profile) }
            hidController.switchProfile(profile)
        }
    }

    fun setKeyboardPreset(preset: KeyboardKeyPreset) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(keyboardPreset = preset) }
        }
    }

    fun setSkin(skin: DendySkin) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(skin = skin) }
        }
    }

    fun setTurboRate(rate: TurboRate) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(turboRate = rate) }
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(hapticEnabled = enabled) }
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(soundEnabled = enabled) }
        }
    }

    fun setSwapAB(swap: Boolean) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(swapAB = swap) }
        }
    }

    fun setEightWayDpad(eightWay: Boolean) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(eightWayDpad = eightWay) }
        }
    }

    fun setKeepScreenOn(keep: Boolean) {
        viewModelScope.launch {
            settingsRepo.update { it.copy(keepScreenOn = keep) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTurboLoop()
        hidController.release()
        wifiController.release()
        soundEngine.release()
    }
}
