package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hid.ConnectionState
import com.example.hid.DiscoveredTvUi
import com.example.hid.GamepadSettings
import com.example.hid.PairedDeviceUi
import com.example.hid.TransportMode
import com.example.hid.WifiConnectionState
import com.example.hid.WifiProtocol
import com.example.ui.theme.DendyAmber
import com.example.ui.theme.DendyLedGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionSheet(
    settings: GamepadSettings,
    connectionState: ConnectionState,
    pairedDevices: List<PairedDeviceUi>,
    wifiState: WifiConnectionState,
    discoveredTvs: List<DiscoveredTvUi>,
    isWifiScanning: Boolean,
    localPhoneIp: String,
    webClientsCount: Int,
    onSelectTransport: (TransportMode) -> Unit,
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onRefreshDevices: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnectBt: () -> Unit,
    onSelectWifiProtocol: (WifiProtocol) -> Unit,
    onSelectWifiPlayer: (Int) -> Unit,
    onStartWifiScan: () -> Unit,
    onSelectDiscoveredTv: (DiscoveredTvUi) -> Unit,
    onPairWithWifiCode: (ip: String, pairPort: Int, code: String, connectPort: Int?) -> Unit,
    onConnectWifi: (ip: String, port: Int, connectPort: Int?) -> Unit,
    onDisconnectWifi: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                        contentDescription = null,
                        tint = when {
                            settings.transportMode == TransportMode.WIFI && wifiState is WifiConnectionState.Connected -> DendyLedGreen
                            settings.transportMode == TransportMode.BLUETOOTH_HID && connectionState is ConnectionState.Connected -> DendyLedGreen
                            else -> DendyAmber
                        },
                    )
                    Text(
                        text = "Подключение к ТВ",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }

                IconButton(
                    onClick = {
                        onRefreshDevices()
                        if (settings.transportMode == TransportMode.WIFI) {
                            onStartWifiScan()
                        }
                    },
                    modifier = Modifier.testTag("refresh_devices_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить",
                    )
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TransportMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.transportMode == mode,
                        onClick = { onSelectTransport(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TransportMode.entries.size,
                        ),
                        modifier = Modifier.testTag("transport_${mode.name}"),
                        icon = {
                            SegmentedButtonDefaults.Icon(active = settings.transportMode == mode) {
                                Icon(
                                    imageVector = if (mode == TransportMode.WIFI) Icons.Default.Wifi else Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        },
                    ) {
                        Text(mode.label)
                    }
                }
            }

            if (settings.transportMode == TransportMode.BLUETOOTH_HID) {
                BluetoothConnectionContent(
                    connectionState = connectionState,
                    pairedDevices = pairedDevices,
                    onRequestPermissions = onRequestPermissions,
                    onRequestEnableBluetooth = onRequestEnableBluetooth,
                    onMakeDiscoverable = onMakeDiscoverable,
                    onOpenBluetoothSettings = onOpenBluetoothSettings,
                    onConnectDevice = onConnectDevice,
                    onDisconnect = onDisconnectBt,
                )
            } else {
                WifiConnectionContent(
                    settings = settings,
                    wifiState = wifiState,
                    discoveredTvs = discoveredTvs,
                    isWifiScanning = isWifiScanning,
                    localPhoneIp = localPhoneIp,
                    webClientsCount = webClientsCount,
                    onSelectWifiProtocol = onSelectWifiProtocol,
                    onSelectWifiPlayer = onSelectWifiPlayer,
                    onStartWifiScan = onStartWifiScan,
                    onSelectDiscoveredTv = onSelectDiscoveredTv,
                    onPairWithWifiCode = onPairWithWifiCode,
                    onConnectWifi = onConnectWifi,
                    onDisconnectWifi = onDisconnectWifi,
                )
            }
        }
    }
}

@Composable
private fun BluetoothConnectionContent(
    connectionState: ConnectionState,
    pairedDevices: List<PairedDeviceUi>,
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
    onMakeDiscoverable: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onConnectDevice: (String) -> Unit,
    onDisconnect: () -> Unit,
) {
    ConnectionStatusBanner(
        connectionState = connectionState,
        onRequestPermissions = onRequestPermissions,
        onRequestEnableBluetooth = onRequestEnableBluetooth,
        onDisconnect = onDisconnect,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Button(
            onClick = onMakeDiscoverable,
            modifier = Modifier
                .weight(1f)
                .testTag("discoverable_button"),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text("Видимость ТВ (120с)")
        }

        OutlinedButton(
            onClick = onOpenBluetoothSettings,
            modifier = Modifier
                .weight(1f)
                .testTag("bt_settings_button"),
        ) {
            Text("Настройки BT")
        }
    }

    Text(
        text = "Сопряжённые устройства",
        style = MaterialTheme.typography.titleMedium,
    )

    if (pairedDevices.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = "Пока нет сопряжённых телевизоров. Нажмите «Видимость ТВ», затем найдите этот телефон в настройках Bluetooth на телевизоре.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            pairedDevices.forEach { device ->
                PairedDeviceRow(
                    device = device,
                    onConnect = { onConnectDevice(device.address) },
                    onDisconnect = onDisconnect,
                )
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Как подключить по Bluetooth:", style = MaterialTheme.typography.titleMedium)
            Text("1. Нажмите «Видимость ТВ (120с)» и подтвердите запрос.", style = MaterialTheme.typography.bodyMedium)
            Text("2. На ТВ откройте: Настройки → Пульты и аксессуары → Добавить устройство.", style = MaterialTheme.typography.bodyMedium)
            Text("3. Выберите имя вашего телефона и подтвердите сопряжение.", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "4. Если ТВ не реагирует в игре — переключите Профиль совместимости в Настройках или используйте вкладку Wi-Fi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WifiConnectionContent(
    settings: GamepadSettings,
    wifiState: WifiConnectionState,
    discoveredTvs: List<DiscoveredTvUi>,
    isWifiScanning: Boolean,
    localPhoneIp: String,
    webClientsCount: Int,
    onSelectWifiProtocol: (WifiProtocol) -> Unit,
    onSelectWifiPlayer: (Int) -> Unit,
    onStartWifiScan: () -> Unit,
    onSelectDiscoveredTv: (DiscoveredTvUi) -> Unit,
    onPairWithWifiCode: (ip: String, pairPort: Int, code: String, connectPort: Int?) -> Unit,
    onConnectWifi: (ip: String, port: Int, connectPort: Int?) -> Unit,
    onDisconnectWifi: () -> Unit,
) {
    var ipText by remember(settings.wifiTargetIp) { mutableStateOf(settings.wifiTargetIp) }
    var portText by remember(settings.wifiPort) { mutableStateOf(settings.wifiPort.toString()) }
    var pairPortText by remember(settings.wifiPairPort) {
        mutableStateOf(if (settings.wifiPairPort > 0) settings.wifiPairPort.toString() else "")
    }
    var connectPortText by remember(settings.wifiConnectPort) {
        mutableStateOf(if (settings.wifiConnectPort > 0) settings.wifiConnectPort.toString() else "")
    }
    var pairingCodeText by remember { mutableStateOf("") }

    WifiStatusBanner(
        wifiState = wifiState,
        localPhoneIp = localPhoneIp,
        webClientsCount = webClientsCount,
        onDisconnect = onDisconnectWifi,
    )

    Text(
        text = "Способ подключения по Wi-Fi",
        style = MaterialTheme.typography.titleMedium,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WifiProtocol.entries.forEach { protocol ->
            val selected = settings.wifiProtocol == protocol
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wifi_protocol_${protocol.name}")
                    .clickable {
                        onSelectWifiProtocol(protocol)
                        portText = protocol.defaultPort.toString()
                    },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                border = if (selected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    RadioButton(
                        selected = selected,
                        onClick = {
                            onSelectWifiProtocol(protocol)
                            portText = protocol.defaultPort.toString()
                        },
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(protocol.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            protocol.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (settings.wifiProtocol == WifiProtocol.ADB_PAIRING_CODE) {
        // Dedicated Android 11+ Wireless Debugging Pairing by 6-digit Code UI
        val pairingDiscovered = discoveredTvs.firstOrNull { it.isAdbPairing }
        val connectDiscovered = discoveredTvs.firstOrNull { it.isAdbConnect }

        if (pairingDiscovered != null || connectDiscovered != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DendyLedGreen.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, DendyLedGreen.copy(alpha = 0.6f)),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "ТВ обнаружен автоматически!",
                        style = MaterialTheme.typography.titleMedium,
                        color = DendyLedGreen,
                    )
                    if (pairingDiscovered != null) {
                        OutlinedButton(
                            onClick = {
                                ipText = pairingDiscovered.ip
                                pairPortText = pairingDiscovered.port.toString()
                                if (connectDiscovered != null) {
                                    connectPortText = connectDiscovered.port.toString()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("autofill_pairing_tv"),
                        ) {
                            Text("Подставить: ${pairingDiscovered.ip}:${pairingDiscovered.port}")
                        }
                    } else if (connectDiscovered != null) {
                        OutlinedButton(
                            onClick = {
                                ipText = connectDiscovered.ip
                                connectPortText = connectDiscovered.port.toString()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("autofill_connect_tv"),
                        ) {
                            Text("Подставить порт подключения: ${connectDiscovered.ip}:${connectDiscovered.port}")
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = ipText,
                onValueChange = { ipText = it },
                label = { Text("IP-адрес ТВ") },
                placeholder = { Text("192.168.1.50") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("wifi_pairing_ip_input"),
            )
            OutlinedTextField(
                value = pairPortText,
                onValueChange = { pairPortText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Порт кода") },
                placeholder = { Text("37421") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("wifi_pairing_port_input"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = pairingCodeText,
                onValueChange = { ch ->
                    if (ch.length <= 8) pairingCodeText = ch.filter { it.isDigit() }
                },
                label = { Text("Код подключения Wi-Fi (6 цифр)") },
                placeholder = { Text("482913") },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1.4f)
                    .testTag("wifi_pairing_code_input"),
            )
            OutlinedTextField(
                value = connectPortText,
                onValueChange = { connectPortText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Порт (опц.)") },
                placeholder = { Text("авто") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("wifi_connect_port_input"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    val pPort = pairPortText.toIntOrNull() ?: 0
                    val cPort = connectPortText.toIntOrNull()
                    onPairWithWifiCode(ipText.trim(), pPort, pairingCodeText.trim(), cPort)
                },
                modifier = Modifier
                    .weight(1.3f)
                    .testTag("wifi_pair_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text("Привязать по коду")
            }
            OutlinedButton(
                onClick = {
                    val cPort = connectPortText.toIntOrNull()
                    onConnectWifi(ipText.trim(), cPort ?: 0, cPort)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("wifi_connect_after_pair_button"),
            ) {
                Text("Подключить")
            }
        }
    } else {
        if (settings.wifiProtocol == WifiProtocol.RETROARCH_UDP) {
            Text(
                text = "Слот игрока (Player 1–4)",
                style = MaterialTheme.typography.titleMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (0..3).forEach { idx ->
                    FilterChip(
                        selected = settings.wifiPlayerIndex == idx,
                        onClick = { onSelectWifiPlayer(idx) },
                        label = { Text("Игрок ${idx + 1}") },
                        modifier = Modifier.testTag("wifi_player_${idx + 1}"),
                    )
                }
            }
        }

        if (settings.wifiProtocol == WifiProtocol.WEB_BRIDGE) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = DendyAmber)
                        Text("Адрес для браузера телевизора:", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "http://$localPhoneIp:${portText.toIntOrNull() ?: 8765}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = DendyLedGreen,
                    )
                    Text(
                        text = "Подключённых браузеров: $webClientsCount • Дополнительно отправляется UDP JSON на порт 55410 (если указан IP ниже).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = ipText,
                onValueChange = { ipText = it },
                label = {
                    Text(
                        if (settings.wifiProtocol == WifiProtocol.WEB_BRIDGE) {
                            "IP ТВ для UDP (необяз.)"
                        } else {
                            "IP-адрес ТВ (напр. 192.168.1.50)"
                        }
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .weight(2f)
                    .testTag("wifi_ip_input"),
            )
            OutlinedTextField(
                value = portText,
                onValueChange = { portText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Порт") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .weight(1f)
                    .testTag("wifi_port_input"),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val isConnected = wifiState is WifiConnectionState.Connected
            Button(
                onClick = {
                    val parsedPort = portText.toIntOrNull() ?: settings.wifiProtocol.defaultPort
                    onConnectWifi(ipText.trim(), parsedPort, null)
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("wifi_connect_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            ) {
                Text(
                    when {
                        settings.wifiProtocol == WifiProtocol.WEB_BRIDGE && isConnected -> "Сервер активен"
                        settings.wifiProtocol == WifiProtocol.WEB_BRIDGE -> "Запустить сервер"
                        isConnected -> "Переподключить"
                        else -> "Подключить по Wi-Fi"
                    }
                )
            }
            if (isConnected) {
                OutlinedButton(
                    onClick = onDisconnectWifi,
                    modifier = Modifier.testTag("wifi_disconnect_button"),
                ) {
                    Text("Отключить")
                }
            }
        }
    }

    // Auto-discovery section
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = "Найденные ТВ в сети Wi-Fi",
            style = MaterialTheme.typography.titleMedium,
        )
        OutlinedButton(
            onClick = onStartWifiScan,
            modifier = Modifier.testTag("wifi_scan_button"),
        ) {
            if (isWifiScanning) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(6.dp))
                Text("Поиск…")
            } else {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Найти ТВ")
            }
        }
    }

    if (discoveredTvs.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Text(
                text = "Нажмите «Найти ТВ», чтобы автоматически обнаружить телевизор (включая порт кода подключения «Отладка по Wi-Fi»), или введите данные с экрана ТВ вручную.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(14.dp),
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            discoveredTvs.forEach { tv ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("discovered_tv_${tv.ip}_${tv.port}")
                        .clickable {
                            ipText = tv.ip
                            if (tv.isAdbPairing) {
                                pairPortText = tv.port.toString()
                            } else if (tv.isAdbConnect) {
                                connectPortText = tv.port.toString()
                            }
                            onSelectDiscoveredTv(tv)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (tv.isAdbPairing) Icons.Default.Key else Icons.Default.Tv,
                            contentDescription = null,
                            tint = if (tv.isAdbPairing) DendyLedGreen else DendyAmber,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tv.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${tv.ip}:${tv.port} • ${tv.serviceType}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("Выбрать", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("Подсказка по настройке ТВ:", style = MaterialTheme.typography.titleMedium)
            when (settings.wifiProtocol) {
                WifiProtocol.ADB_PAIRING_CODE -> {
                    Text("1. На ТВ откройте: Настройки → Система → Для разработчиков → Отладка по Wi-Fi (включите).", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Нажмите «Подключить устройство с помощью кода подключения».", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Введите IP, 5-значный порт рядом с кодом и 6-значный код выше (или нажмите «Найти ТВ» для автозаполнения), затем нажмите «Привязать по коду».", style = MaterialTheme.typography.bodyMedium)
                    Text("4. Поле «Порт (опц.)» можно оставить пустым — либо указать порт с главного экрана «Отладка по Wi-Fi» (например, 41289) для быстрого переподключения.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                WifiProtocol.RETROARCH_UDP -> {
                    Text("• На ТВ в RetroArch включите: Настройки → Ввод → Сетевой геймпад (Network Gamepad) = Вкл.", style = MaterialTheme.typography.bodyMedium)
                    Text("• Для быстрых команд (Меню/Пауза/Громкость) также включите: Сеть → Сетевые команды = Вкл.", style = MaterialTheme.typography.bodyMedium)
                }

                WifiProtocol.ADB_ANDROID_TV -> {
                    Text("• На Android TV включите «Отладка по сети» (порт 5555) или введите порт из «Отладка по Wi-Fi» (если устройство уже привязано по коду).", style = MaterialTheme.typography.bodyMedium)
                }

                WifiProtocol.WEB_BRIDGE -> {
                    Text("• Запустите сервер и откройте указанный адрес http://... на телевизоре в любом браузере (подходит для LG webOS, Samsung Tizen, Smart TV).", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun WifiStatusBanner(
    wifiState: WifiConnectionState,
    localPhoneIp: String,
    webClientsCount: Int,
    onDisconnect: () -> Unit,
) {
    val (title, subtitle, showDisconnect) = when (wifiState) {
        WifiConnectionState.Disconnected -> Triple(
            "Wi-Fi готов к подключению",
            "IP телефона: $localPhoneIp. Выберите ТВ или введите данные с экрана ТВ.",
            false,
        )

        WifiConnectionState.NoWifi -> Triple(
            "Нет подключения к Wi-Fi",
            "Подключите телефон к той же Wi-Fi сети, что и телевизор.",
            false,
        )

        is WifiConnectionState.PairingWithCode -> Triple(
            "Привязка по коду к ${wifiState.targetLabel}…",
            "Выполняется защищённый TLS/SPAKE2 обмен ключами.",
            false,
        )

        is WifiConnectionState.Connecting -> Triple(
            "Подключение к ${wifiState.targetLabel}…",
            "Устанавливаем сетевой канал управления.",
            false,
        )

        is WifiConnectionState.AwaitingTvApproval -> Triple(
            "Подтвердите на экране ТВ (${wifiState.tvIp})",
            "На телевизоре появилось окно «Разрешить отладку по сети» — нажмите «Разрешить».",
            true,
        )

        is WifiConnectionState.PairedReadyToConnect -> Triple(
            "Код принят для ${wifiState.tvIp}",
            wifiState.message,
            false,
        )

        is WifiConnectionState.Connected -> Triple(
            "Wi-Fi активен: ${wifiState.targetLabel}",
            if (wifiState.protocol == WifiProtocol.WEB_BRIDGE) {
                "${wifiState.detail} • Подключено экранов: $webClientsCount"
            } else {
                wifiState.detail
            },
            true,
        )

        is WifiConnectionState.Error -> Triple(
            "Ошибка Wi-Fi",
            wifiState.message,
            false,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showDisconnect) {
                OutlinedButton(onClick = onDisconnect) {
                    Text("Стоп")
                }
            }
        }
    }
}

@Composable
private fun ConnectionStatusBanner(
    connectionState: ConnectionState,
    onRequestPermissions: () -> Unit,
    onRequestEnableBluetooth: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val (title, subtitle, actionLabel, onAction) = when (connectionState) {
        ConnectionState.PermissionRequired -> Quad(
            "Нужен доступ к Bluetooth",
            "Разрешите поиск и подключение к ближайшим устройствам, чтобы телефон работал как геймпад.",
            "Разрешить",
            onRequestPermissions,
        )

        ConnectionState.BluetoothDisabled -> Quad(
            "Bluetooth выключен",
            "Включите Bluetooth на телефоне для эмуляции беспроводного геймпада.",
            "Включить",
            onRequestEnableBluetooth,
        )

        ConnectionState.Registering -> Quad(
            "Инициализация HID…",
            "Регистрируем профиль беспроводного геймпада Dendy в системе Android.",
            null,
            null,
        )

        ConnectionState.ReadyToPair -> Quad(
            "Готов к подключению",
            "Выберите телевизор из списка ниже или выполните новое сопряжение в меню ТВ.",
            null,
            null,
        )

        is ConnectionState.Connecting -> Quad(
            "Подключение к ${connectionState.deviceName}…",
            "Ожидаем подтверждения HID-канала от телевизора.",
            null,
            null,
        )

        is ConnectionState.Connected -> Quad(
            "Подключено: ${connectionState.deviceName}",
            "Геймпад активен и передаёт нажатия на ТВ.",
            "Отключить",
            onDisconnect,
        )

        is ConnectionState.UnsupportedApi -> Quad(
            "Bluetooth HID недоступен",
            connectionState.message,
            null,
            null,
        )

        is ConnectionState.Error -> Quad(
            "Ошибка соединения",
            connectionState.message,
            null,
            null,
        )

        ConnectionState.Idle -> Quad(
            "Ожидание",
            "Подготовка Bluetooth HID.",
            null,
            null,
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (actionLabel != null && onAction != null) {
                Button(
                    onClick = onAction,
                    modifier = Modifier.testTag("connection_banner_action"),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun PairedDeviceRow(
    device: PairedDeviceUi,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (device.isConnected) {
            DendyLedGreen.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        border = if (device.isConnected) {
            BorderStroke(1.dp, DendyLedGreen)
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = if (device.isLikelyTvOrPc) Icons.Default.Tv else Icons.Default.Computer,
                contentDescription = null,
                tint = if (device.isConnected) DendyLedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (device.isConnected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Подключено",
                            tint = DendyLedGreen,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (device.isConnected) {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.testTag("disconnect_device_${device.address}"),
                ) {
                    Text("Отключить")
                }
            } else {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.testTag("connect_device_${device.address}"),
                ) {
                    Text("Подключить")
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
)
