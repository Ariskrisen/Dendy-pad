package com.example.wifi

import android.content.Context
import com.example.hid.DiscoveredTvUi
import com.example.hid.GamepadSettings
import com.example.hid.NesButton
import com.example.hid.TvAction
import com.example.hid.WifiConnectionState
import com.example.hid.WifiProtocol
import java.util.concurrent.Executors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiGamepadController(context: Context) {

    private val discovery = WifiTvDiscovery(context)
    private val retroArch = RetroArchWifiClient()
    private val adbClient = AdbWifiClient(context)
    private val webBridge = WifiWebBridgeServer()
    private val executor = Executors.newSingleThreadExecutor()

    val discoveredTvs: StateFlow<List<DiscoveredTvUi>> = discovery.discoveredTvs
    val isScanning: StateFlow<Boolean> = discovery.isScanning
    val webClientsCount: StateFlow<Int> = webBridge.connectedClients

    private val _wifiState = MutableStateFlow<WifiConnectionState>(WifiConnectionState.Disconnected)
    val wifiState: StateFlow<WifiConnectionState> = _wifiState.asStateFlow()

    private val _localPhoneIp = MutableStateFlow("0.0.0.0")
    val localPhoneIp: StateFlow<String> = _localPhoneIp.asStateFlow()

    private val _lastWifiPacket = MutableStateFlow("WI-FI IDLE")
    val lastWifiPacket: StateFlow<String> = _lastWifiPacket.asStateFlow()

    private val _packetCount = MutableStateFlow(0L)
    val packetCount: StateFlow<Long> = _packetCount.asStateFlow()

    fun refreshLocalNetwork() {
        _localPhoneIp.value = discovery.getLocalIpv4Address()
        if (!discovery.isWifiConnected()) {
            _wifiState.value = WifiConnectionState.NoWifi
        } else if (_wifiState.value is WifiConnectionState.NoWifi) {
            _wifiState.value = WifiConnectionState.Disconnected
        }
    }

    fun startTvScan() {
        refreshLocalNetwork()
        if (discovery.isWifiConnected()) {
            discovery.startDiscovery()
        }
    }

    fun stopTvScan() {
        discovery.stopDiscovery()
    }

    fun pairWithCodeAndConnect(
        ip: String,
        pairPort: Int,
        pairingCode: String,
        connectPort: Int?,
    ) {
        refreshLocalNetwork()
        if (!discovery.isWifiConnected()) {
            _wifiState.value = WifiConnectionState.NoWifi
            return
        }
        if (ip.isBlank() || pairPort <= 0 || pairingCode.length < 4) {
            _wifiState.value = WifiConnectionState.Error(
                "Укажите IP-адрес ТВ, порт из окна кода и 6-значный код подключения."
            )
            return
        }
        adbClient.pairAndConnect(
            ip = ip,
            pairPort = pairPort,
            pairingCode = pairingCode,
            connectPort = connectPort,
            onPairingStarted = {
                _wifiState.value = WifiConnectionState.PairingWithCode("$ip:$pairPort")
            },
            onPairedConnecting = {
                _wifiState.value = WifiConnectionState.Connecting(ip)
            },
            onPairedNeedConnectPort = { msg ->
                _wifiState.value = WifiConnectionState.PairedReadyToConnect(ip, msg)
            },
            onConnected = { target ->
                _wifiState.value = WifiConnectionState.Connected(
                    targetLabel = target,
                    protocol = WifiProtocol.ADB_PAIRING_CODE,
                    detail = "Отладка по Wi-Fi (TLS подключено)",
                )
            },
            onError = { err ->
                _wifiState.value = WifiConnectionState.Error(err)
            },
        )
    }

    fun connect(settings: GamepadSettings) {
        refreshLocalNetwork()
        if (!discovery.isWifiConnected()) {
            _wifiState.value = WifiConnectionState.NoWifi
            return
        }

        val ip = settings.wifiTargetIp.trim()
        when (settings.wifiProtocol) {
            WifiProtocol.ADB_PAIRING_CODE -> {
                if (ip.isEmpty()) {
                    _wifiState.value = WifiConnectionState.Error("Укажите IP-адрес телевизора")
                    return
                }
                val connectPort = settings.wifiConnectPort.takeIf { it > 0 }
                    ?: discoveredTvs.value.firstOrNull { it.ip == ip && it.isAdbConnect }?.port
                if (connectPort == null || connectPort <= 0) {
                    _wifiState.value = WifiConnectionState.Error(
                        "Укажите «Порт подключения» с главного экрана «Отладка по Wi-Fi» или выполните привязку по коду."
                    )
                    return
                }
                _wifiState.value = WifiConnectionState.Connecting("$ip:$connectPort")
                adbClient.connectDirect(
                    ip = ip,
                    port = connectPort,
                    onConnected = { target ->
                        _wifiState.value = WifiConnectionState.Connected(
                            targetLabel = target,
                            protocol = WifiProtocol.ADB_PAIRING_CODE,
                            detail = "Отладка по Wi-Fi (TLS подключено)",
                        )
                    },
                    onError = { msg ->
                        _wifiState.value = WifiConnectionState.Error(msg)
                    },
                )
            }

            WifiProtocol.RETROARCH_UDP -> {
                if (ip.isEmpty()) {
                    _wifiState.value = WifiConnectionState.Error("Укажите IP-адрес телевизора или выберите ТВ из списка")
                    return
                }
                retroArch.resetState()
                _wifiState.value = WifiConnectionState.Connecting(ip)
                executor.execute {
                    val ping = retroArch.pingRetroArch(ip)
                    val versionLabel = ping.getOrNull()
                    _wifiState.value = WifiConnectionState.Connected(
                        targetLabel = ip,
                        protocol = WifiProtocol.RETROARCH_UDP,
                        detail = if (versionLabel != null) {
                            "Ответ: $versionLabel (UDP ${settings.wifiPort + settings.wifiPlayerIndex})"
                        } else {
                            "UDP-поток активен на порт ${settings.wifiPort + settings.wifiPlayerIndex}"
                        },
                    )
                }
            }

            WifiProtocol.ADB_ANDROID_TV -> {
                if (ip.isEmpty()) {
                    _wifiState.value = WifiConnectionState.Error("Укажите IP-адрес Android TV")
                    return
                }
                _wifiState.value = WifiConnectionState.Connecting("$ip:${settings.wifiPort}")
                adbClient.connectDirect(
                    ip = ip,
                    port = settings.wifiPort,
                    onConnected = { target ->
                        _wifiState.value = WifiConnectionState.Connected(
                            targetLabel = target,
                            protocol = WifiProtocol.ADB_ANDROID_TV,
                            detail = "ADB Wi-Fi ($target)",
                        )
                    },
                    onError = { msg ->
                        _wifiState.value = WifiConnectionState.Error(msg)
                    },
                )
            }

            WifiProtocol.WEB_BRIDGE -> {
                val phoneIp = discovery.getLocalIpv4Address()
                _localPhoneIp.value = phoneIp
                webBridge.start(settings.wifiPort) { err ->
                    _wifiState.value = WifiConnectionState.Error(err)
                }
                _wifiState.value = WifiConnectionState.Connected(
                    targetLabel = "http://$phoneIp:${settings.wifiPort}",
                    protocol = WifiProtocol.WEB_BRIDGE,
                    detail = "Откройте адрес в браузере ТВ",
                )
            }
        }
    }

    fun disconnect() {
        retroArch.close()
        adbClient.disconnect()
        webBridge.stop()
        refreshLocalNetwork()
        if (discovery.isWifiConnected()) {
            _wifiState.value = WifiConnectionState.Disconnected
        }
    }

    fun sendGamepadState(
        pressed: Set<NesButton>,
        turboOnPhase: Boolean,
        settings: GamepadSettings,
    ) {
        val state = _wifiState.value
        if (state !is WifiConnectionState.Connected) return

        executor.execute {
            val summary = when (settings.wifiProtocol) {
                WifiProtocol.RETROARCH_UDP -> retroArch.sendGamepadState(
                    ip = settings.wifiTargetIp,
                    basePort = settings.wifiPort,
                    playerIndex = settings.wifiPlayerIndex,
                    pressed = pressed,
                    turboOnPhase = turboOnPhase,
                    swapAB = settings.swapAB,
                )

                WifiProtocol.ADB_PAIRING_CODE,
                WifiProtocol.ADB_ANDROID_TV -> {
                    adbClient.sendGamepadState(pressed, settings.swapAB)
                    "ADB WI-FI [${pressed.joinToString("+") { it.name }.ifEmpty { "IDLE" }}]"
                }

                WifiProtocol.WEB_BRIDGE -> webBridge.broadcastState(
                    pressed = pressed,
                    turboOnPhase = turboOnPhase,
                    swapAB = settings.swapAB,
                    targetIp = settings.wifiTargetIp.takeIf { it.isNotBlank() },
                )
            }
            _lastWifiPacket.value = summary
            _packetCount.value = _packetCount.value + 1L
        }
    }

    fun sendTvAction(action: TvAction, settings: GamepadSettings) {
        if (_wifiState.value !is WifiConnectionState.Connected) return
        executor.execute {
            when (settings.wifiProtocol) {
                WifiProtocol.RETROARCH_UDP -> retroArch.sendTvCommand(settings.wifiTargetIp, action)
                WifiProtocol.ADB_PAIRING_CODE,
                WifiProtocol.ADB_ANDROID_TV -> adbClient.sendTvAction(action)
                WifiProtocol.WEB_BRIDGE -> webBridge.broadcastTvAction(
                    action = action,
                    targetIp = settings.wifiTargetIp.takeIf { it.isNotBlank() },
                )
            }
        }
    }

    fun release() {
        discovery.stopDiscovery()
        retroArch.close()
        adbClient.disconnect()
        webBridge.stop()
        executor.shutdownNow()
    }
}
