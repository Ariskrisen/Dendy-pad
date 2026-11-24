package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.hid.CompatibilityProfile
import com.example.hid.ConnectionState
import com.example.hid.GamepadSettings
import com.example.hid.NesButton
import com.example.hid.PairedDeviceUi
import com.example.hid.TransportMode
import com.example.hid.WifiConnectionState
import com.example.hid.WifiProtocol
import com.example.ui.components.ConnectionSheet
import com.example.ui.components.DendyControllerCanvas
import com.example.ui.components.SettingsSheet
import com.example.ui.components.TvRemoteSheet
import com.example.ui.theme.DendyAmber
import com.example.ui.theme.DendyCrimson
import com.example.ui.theme.DendyCrimsonBright
import com.example.ui.theme.DendyLedGreen
import com.example.ui.theme.DendySurfaceDark
import com.example.ui.theme.PressStartFontFamily

@Composable
fun DendyPadScreen(
    viewModel: GamepadViewModel,
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
    val wifiState by viewModel.wifiState.collectAsStateWithLifecycle()
    val discoveredTvs by viewModel.discoveredTvs.collectAsStateWithLifecycle()
    val isWifiScanning by viewModel.isWifiScanning.collectAsStateWithLifecycle()
    val localPhoneIp by viewModel.localPhoneIp.collectAsStateWithLifecycle()
    val webClientsCount by viewModel.webClientsCount.collectAsStateWithLifecycle()
    val pressedButtons by viewModel.pressedButtons.collectAsStateWithLifecycle()
    val turboPhaseOn by viewModel.turboPhaseOn.collectAsStateWithLifecycle()
    val lastReportHex by viewModel.lastReportHex.collectAsStateWithLifecycle()
    val btPacketCount by viewModel.btPacketCount.collectAsStateWithLifecycle()
    val lastWifiPacket by viewModel.lastWifiPacket.collectAsStateWithLifecycle()
    val wifiPacketCount by viewModel.wifiPacketCount.collectAsStateWithLifecycle()

    var showConnectionSheet by rememberSaveable { mutableStateOf(false) }
    var showSettingsSheet by rememberSaveable { mutableStateOf(false) }
    var showTvRemoteSheet by rememberSaveable { mutableStateOf(false) }

    val view = LocalView.current
    DisposableEffect(settings.keepScreenOn) {
        view.keepScreenOn = settings.keepScreenOn
        onDispose { view.keepScreenOn = false }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        viewModel.clearAllButtons()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onPermissionsOrBtChanged()
    }

    val isActiveConnected = when (settings.transportMode) {
        TransportMode.BLUETOOTH_HID -> connectionState is ConnectionState.Connected
        TransportMode.WIFI -> wifiState is WifiConnectionState.Connected
    }

    val liveTelemetryText = if (settings.transportMode == TransportMode.WIFI) lastWifiPacket else lastReportHex
    val activePacketCount = if (settings.transportMode == TransportMode.WIFI) wifiPacketCount else btPacketCount

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1C191E), DendySurfaceDark, Color(0xFF100E11)),
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > maxHeight
            val wideLandscape = maxWidth >= 680.dp

            if (isLandscape) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    LandscapeTopBar(
                        connectionState = connectionState,
                        wifiState = wifiState,
                        settings = settings,
                        liveTelemetry = liveTelemetryText,
                        showHexMonitor = wideLandscape,
                        onOpenConnection = {
                            viewModel.refreshDevices()
                            showConnectionSheet = true
                        },
                        onOpenRemote = { showTvRemoteSheet = true },
                        onOpenSettings = { showSettingsSheet = true },
                    )
                    DendyControllerCanvas(
                        pressedButtons = pressedButtons,
                        turboPhaseOn = turboPhaseOn,
                        skin = settings.skin,
                        eightWayDpad = settings.eightWayDpad,
                        swapAB = settings.swapAB,
                        isConnected = isActiveConnected,
                        onButtonsChanged = viewModel::updateButtons,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    PortraitTvHubDeck(
                        connectionState = connectionState,
                        wifiState = wifiState,
                        pairedDevices = pairedDevices,
                        settings = settings,
                        pressedButtons = pressedButtons,
                        liveTelemetry = liveTelemetryText,
                        packetCount = activePacketCount,
                        localPhoneIp = localPhoneIp,
                        onSelectTransport = viewModel::setTransportMode,
                        onRequestPermissions = onRequestPermissions,
                        onRequestEnableBluetooth = onRequestEnableBluetooth,
                        onMakeDiscoverable = onMakeDiscoverable,
                        onConnectDevice = viewModel::connectToDevice,
                        onSelectProfile = viewModel::setCompatibilityProfile,
                        onSelectWifiProtocol = viewModel::setWifiProtocol,
                        onQuickConnectWifi = { viewModel.connectWifi() },
                        onOpenConnection = {
                            viewModel.refreshDevices()
                            showConnectionSheet = true
                        },
                        onOpenRemote = { showTvRemoteSheet = true },
                        onOpenSettings = { showSettingsSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    DendyControllerCanvas(
                        pressedButtons = pressedButtons,
                        turboPhaseOn = turboPhaseOn,
                        skin = settings.skin,
                        eightWayDpad = settings.eightWayDpad,
                        swapAB = settings.swapAB,
                        isConnected = isActiveConnected,
                        onButtonsChanged = viewModel::updateButtons,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    )
                }
            }
        }
    }

    if (showConnectionSheet) {
        ConnectionSheet(
            settings = settings,
            connectionState = connectionState,
            pairedDevices = pairedDevices,
            wifiState = wifiState,
            discoveredTvs = discoveredTvs,
            isWifiScanning = isWifiScanning,
            localPhoneIp = localPhoneIp,
            webClientsCount = webClientsCount,
            onSelectTransport = viewModel::setTransportMode,
            onRequestPermissions = onRequestPermissions,
            onRequestEnableBluetooth = onRequestEnableBluetooth,
            onMakeDiscoverable = onMakeDiscoverable,
            onOpenBluetoothSettings = onOpenBluetoothSettings,
            onRefreshDevices = viewModel::refreshDevices,
            onConnectDevice = viewModel::connectToDevice,
            onDisconnectBt = viewModel::disconnectDevice,
            onSelectWifiProtocol = viewModel::setWifiProtocol,
            onSelectWifiPlayer = viewModel::setWifiPlayerIndex,
            onStartWifiScan = viewModel::startWifiTvScan,
            onSelectDiscoveredTv = viewModel::selectDiscoveredTv,
            onPairWithWifiCode = viewModel::pairWithWifiCode,
            onConnectWifi = { ip, port, connectPort -> viewModel.connectWifi(ip, port, connectPort) },
            onDisconnectWifi = viewModel::disconnectWifi,
            onDismiss = {
                viewModel.stopWifiTvScan()
                showConnectionSheet = false
            },
        )
    }

    if (showSettingsSheet) {
        SettingsSheet(
            settings = settings,
            onSelectProfile = viewModel::setCompatibilityProfile,
            onSelectKeyboardPreset = viewModel::setKeyboardPreset,
            onSelectSkin = viewModel::setSkin,
            onSelectTurboRate = viewModel::setTurboRate,
            onToggleHaptic = viewModel::setHapticEnabled,
            onToggleSound = viewModel::setSoundEnabled,
            onToggleSwapAB = viewModel::setSwapAB,
            onToggleEightWay = viewModel::setEightWayDpad,
            onToggleKeepScreenOn = viewModel::setKeepScreenOn,
            onDismiss = { showSettingsSheet = false },
        )
    }

    if (showTvRemoteSheet) {
        TvRemoteSheet(
            onTvAction = viewModel::sendTvAction,
            onDismiss = { showTvRemoteSheet = false },
        )
    }
}

@Composable
private fun LandscapeTopBar(
    connectionState: ConnectionState,
    wifiState: WifiConnectionState,
    settings: GamepadSettings,
    liveTelemetry: String,
    showHexMonitor: Boolean,
    onOpenConnection: () -> Unit,
    onOpenRemote: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val isConnected = when (settings.transportMode) {
        TransportMode.BLUETOOTH_HID -> connectionState is ConnectionState.Connected
        TransportMode.WIFI -> wifiState is WifiConnectionState.Connected
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        StatusPillButton(
            connectionState = connectionState,
            wifiState = wifiState,
            settings = settings,
            onClick = onOpenConnection,
            modifier = Modifier.weight(1f, fill = false),
        )

        if (showHexMonitor) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                Text(
                    text = liveTelemetry,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            IconButton(
                onClick = onOpenRemote,
                modifier = Modifier.testTag("toolbar_tv_remote"),
            ) {
                Icon(
                    imageVector = Icons.Default.SettingsRemote,
                    contentDescription = "Пульт ТВ",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = onOpenConnection,
                modifier = Modifier.testTag("toolbar_connection"),
            ) {
                Icon(
                    imageVector = if (settings.transportMode == TransportMode.WIFI) {
                        Icons.Default.Wifi
                    } else {
                        Icons.Default.Bluetooth
                    },
                    contentDescription = "Подключение к ТВ (Bluetooth / Wi-Fi)",
                    tint = if (isConnected) DendyLedGreen else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.testTag("toolbar_settings"),
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Настройки",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun StatusPillButton(
    connectionState: ConnectionState,
    wifiState: WifiConnectionState,
    settings: GamepadSettings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dotColor by animateColorAsState(
        targetValue = when (settings.transportMode) {
            TransportMode.BLUETOOTH_HID -> when (connectionState) {
                is ConnectionState.Connected -> DendyLedGreen
                is ConnectionState.Connecting,
                ConnectionState.Registering,
                ConnectionState.ReadyToPair -> DendyAmber
                else -> DendyCrimsonBright
            }

            TransportMode.WIFI -> when (wifiState) {
                is WifiConnectionState.Connected -> DendyLedGreen
                is WifiConnectionState.PairingWithCode,
                is WifiConnectionState.Connecting,
                is WifiConnectionState.AwaitingTvApproval,
                is WifiConnectionState.PairedReadyToConnect,
                WifiConnectionState.Disconnected -> DendyAmber
                else -> DendyCrimsonBright
            }
        },
        label = "statusDot",
    )

    val label = when (settings.transportMode) {
        TransportMode.BLUETOOTH_HID -> when (connectionState) {
            is ConnectionState.Connected -> "BT: ${connectionState.deviceName}"
            is ConnectionState.Connecting -> "BT: ${connectionState.deviceName}…"
            ConnectionState.ReadyToPair -> "Подключить ТВ (BT)"
            ConnectionState.Registering -> "HID запуск…"
            ConnectionState.BluetoothDisabled -> "Включить Bluetooth"
            ConnectionState.PermissionRequired -> "Нужен доступ BT"
            is ConnectionState.UnsupportedApi -> "Нет HID API"
            is ConnectionState.Error -> "Ошибка BT"
            ConnectionState.Idle -> "Dendy HID"
        }

        TransportMode.WIFI -> when (wifiState) {
            is WifiConnectionState.Connected -> "Wi-Fi: ${wifiState.targetLabel}"
            is WifiConnectionState.PairingWithCode -> "Код Wi-Fi: ${wifiState.targetLabel}…"
            is WifiConnectionState.Connecting -> "Wi-Fi: ${wifiState.targetLabel}…"
            is WifiConnectionState.AwaitingTvApproval -> "Подтвердите на ТВ!"
            is WifiConnectionState.PairedReadyToConnect -> "Код принят (${wifiState.tvIp})"
            WifiConnectionState.NoWifi -> "Нет Wi-Fi сети"
            is WifiConnectionState.Error -> "Ошибка Wi-Fi"
            WifiConnectionState.Disconnected -> "Подключить ТВ (Wi-Fi)"
        }
    }

    val badge = if (settings.transportMode == TransportMode.WIFI) {
        settings.wifiProtocol.shortBadge
    } else {
        settings.compatibilityProfile.shortBadge
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("status_pill_button"),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = DendyCrimson.copy(alpha = 0.25f),
            ) {
                Text(
                    text = if (settings.swapAB) "$badge • A↔B" else badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortraitTvHubDeck(
    connectionState: ConnectionState,
    wifiState: WifiConnectionState,
    pairedDevices: List<PairedDeviceUi>,
    settings: GamepadSettings,
    pressedButtons: Set<NesButton>,
    liveTelemetry: String,
    packetCount: Long,
    localPhoneIp: String,
    onSelectTransport: (TransportMode) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onSelectProfile: (CompatibilityProfile) -> Unit,
    onSelectWifiProtocol: (WifiProtocol) -> Unit,
    onQuickConnectWifi: () -> Unit,
    onOpenConnection: () -> Unit,
    onOpenRemote: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isConnected = when (settings.transportMode) {
        TransportMode.BLUETOOTH_HID -> connectionState is ConnectionState.Connected
        TransportMode.WIFI -> wifiState is WifiConnectionState.Connected
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "DENDY PAD",
                    fontFamily = PressStartFontFamily,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    text = "BLUETOOTH HID & WI-FI ДЛЯ ТВ",
                    style = MaterialTheme.typography.labelSmall,
                    color = DendyAmber,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onOpenRemote,
                    modifier = Modifier.testTag("portrait_tv_remote"),
                ) {
                    Icon(Icons.Default.SettingsRemote, contentDescription = "Пульт ТВ")
                }
                IconButton(
                    onClick = onOpenConnection,
                    modifier = Modifier.testTag("portrait_connection"),
                ) {
                    Icon(
                        imageVector = if (settings.transportMode == TransportMode.WIFI) {
                            Icons.Default.Wifi
                        } else {
                            when (connectionState) {
                                is ConnectionState.Connected -> Icons.Default.BluetoothConnected
                                is ConnectionState.BluetoothDisabled -> Icons.Default.BluetoothDisabled
                                else -> Icons.Default.Bluetooth
                            }
                        },
                        contentDescription = "Подключение к ТВ",
                        tint = if (isConnected) DendyLedGreen else MaterialTheme.colorScheme.onSurface,
                    )
                }
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("portrait_settings"),
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Настройки")
                }
            }
        }

        // Main Console Connection Deck Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Quick Transport Switcher
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    TransportMode.entries.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = settings.transportMode == mode,
                            onClick = { onSelectTransport(mode) },
                            shape = SegmentedButtonDefaults.itemShape(idx, TransportMode.entries.size),
                            modifier = Modifier.testTag("portrait_transport_${mode.name}"),
                        ) {
                            Text(mode.label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    StatusPillButton(
                        connectionState = connectionState,
                        wifiState = wifiState,
                        settings = settings,
                        onClick = onOpenConnection,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "TX: $packetCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (settings.transportMode == TransportMode.BLUETOOTH_HID) {
                    when (connectionState) {
                        ConnectionState.PermissionRequired -> {
                            Button(
                                onClick = onRequestPermissions,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("portrait_cta_permissions"),
                                colors = ButtonDefaults.buttonColors(containerColor = DendyCrimsonBright),
                            ) {
                                Text("Разрешить доступ Bluetooth")
                            }
                        }

                        ConnectionState.BluetoothDisabled -> {
                            Button(
                                onClick = onRequestEnableBluetooth,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("portrait_cta_enable_bt"),
                                colors = ButtonDefaults.buttonColors(containerColor = DendyCrimsonBright),
                            ) {
                                Text("Включить Bluetooth")
                            }
                        }

                        ConnectionState.ReadyToPair, is ConnectionState.Error -> {
                            val quickTarget = pairedDevices.firstOrNull { !it.isConnected }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Button(
                                    onClick = onMakeDiscoverable,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("portrait_cta_discoverable"),
                                    colors = ButtonDefaults.buttonColors(containerColor = DendyCrimsonBright),
                                ) {
                                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Видимость ТВ")
                                }
                                if (quickTarget != null) {
                                    Button(
                                        onClick = { onConnectDevice(quickTarget.address) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("portrait_cta_quick_connect"),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    ) {
                                        Text(quickTarget.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }

                        else -> {}
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        CompatibilityProfile.entries.forEach { profile ->
                            FilterChip(
                                selected = settings.compatibilityProfile == profile,
                                onClick = { onSelectProfile(profile) },
                                label = { Text(profile.shortBadge, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("quick_profile_${profile.name}"),
                            )
                        }
                    }
                } else {
                    // Wi-Fi Quick Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = onOpenConnection,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("portrait_wifi_setup"),
                            colors = ButtonDefaults.buttonColors(containerColor = DendyCrimsonBright),
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                if (settings.wifiTargetIp.isBlank() && settings.wifiProtocol != WifiProtocol.WEB_BRIDGE) {
                                    "Настроить Wi-Fi ТВ"
                                } else {
                                    "Поиск / IP ТВ"
                                }
                            )
                        }
                        if (settings.wifiProtocol == WifiProtocol.WEB_BRIDGE || settings.wifiTargetIp.isNotBlank()) {
                            Button(
                                onClick = onQuickConnectWifi,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("portrait_wifi_quick_connect"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            ) {
                                Text(
                                    if (settings.wifiProtocol == WifiProtocol.WEB_BRIDGE) {
                                        "Старт :${settings.wifiPort}"
                                    } else {
                                        settings.wifiTargetIp
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WifiProtocol.entries.forEach { proto ->
                            FilterChip(
                                selected = settings.wifiProtocol == proto,
                                onClick = { onSelectWifiProtocol(proto) },
                                label = { Text(proto.shortBadge, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("quick_wifi_${proto.name}"),
                            )
                        }
                    }

                    if (settings.wifiProtocol == WifiProtocol.WEB_BRIDGE) {
                        Text(
                            text = "Откройте в браузере ТВ: http://$localPhoneIp:${settings.wifiPort}",
                            style = MaterialTheme.typography.labelSmall,
                            color = DendyAmber,
                        )
                    }
                }

                // Live Signal Telemetry Strip
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = liveTelemetry,
                            style = MaterialTheme.typography.labelSmall,
                            color = DendyLedGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (pressedButtons.isEmpty()) {
                                "Поверните экран для игры"
                            } else {
                                pressedButtons.joinToString("+") { it.name }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
