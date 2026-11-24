package com.example.hid

enum class NesButton {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    SELECT,
    START,
    B,
    A,
    TURBO_B,
    TURBO_A,
}

enum class TvAction {
    HOME,
    BACK,
    MENU,
    VOL_UP,
    VOL_DOWN,
    MUTE,
    PLAY_PAUSE,
}

enum class TransportMode(val label: String) {
    BLUETOOTH_HID("Bluetooth HID"),
    WIFI("Wi-Fi"),
}

enum class WifiProtocol(
    val title: String,
    val shortBadge: String,
    val defaultPort: Int,
    val description: String,
) {
    ADB_PAIRING_CODE(
        title = "По коду Wi-Fi (Android TV 11+)",
        shortBadge = "КОД WI-FI",
        defaultPort = 37000,
        description = "Подключение через «Отладка по Wi-Fi» по 6-значному коду и динамическому порту (Android TV / Google TV 11+ без кабеля и лишних программ).",
    ),
    RETROARCH_UDP(
        title = "RetroArch Network Pad (UDP)",
        shortBadge = "RETROARCH",
        defaultPort = 55400,
        description = "Прямой протокол RetroArch (порт 55400 + команды 55355). Минимальная задержка <2 мс, поддержка 1–4 игроков на Android TV, Shield, Mi Box и ПК.",
    ),
    ADB_ANDROID_TV(
        title = "ADB Wi-Fi прямой порт (5555 / TLS)",
        shortBadge = "ADB ПОРТ",
        defaultPort = 5555,
        description = "Прямое подключение к порту ADB (5555 или порт TLS после привязки по коду).",
    ),
    WEB_BRIDGE(
        title = "Smart TV Браузер + WebSocket / UDP JSON",
        shortBadge = "WEB/JSON",
        defaultPort = 8765,
        description = "Встроенный веб-сервер (http://IP:8765) для Smart TV браузеров (LG webOS, Samsung Tizen) и открытый WebSocket / UDP JSON поток (порт 55410).",
    ),
}

enum class CompatibilityProfile(
    val title: String,
    val shortBadge: String,
    val description: String,
) {
    UNIVERSAL_TV(
        title = "Универсальный ТВ (Рекомендуется)",
        shortBadge = "УНИВЕРСАЛ",
        description = "Геймпад (Hat + оси X/Y + 16 кнопок) + пульт ТВ. Идеально для Android TV, Google TV, Mi Box, Fire TV и RetroArch.",
    ),
    DUAL_HYBRID(
        title = "Гибрид: Геймпад + Клавиатура",
        shortBadge = "ГИБРИД",
        description = "Одновременно отправляет сигналы геймпада и клавиатуры. Максимум совместимости для нестандартных ТВ-приставок.",
    ),
    PURE_GAMEPAD_XY(
        title = "Чистый Геймпад (только X/Y)",
        shortBadge = "X/Y PAD",
        description = "Без Hat-переключателя. Выберите, если в эмуляторе или меню ТВ срабатывают двойные нажатия крестовины.",
    ),
    SMART_TV_KEYBOARD(
        title = "Клавиатура Smart TV / Браузер",
        shortBadge = "КЛАВИАТУРА",
        description = "Профиль HID-клавиатуры для телевизоров LG webOS, Samsung Tizen и браузерных эмуляторов.",
    ),
}

enum class KeyboardKeyPreset(
    val title: String,
    val description: String,
) {
    RETROARCH_DEFAULT(
        title = "RetroArch / Эмуляторы (Z / X)",
        description = "Стрелки, A = Z, B = X, Start = Enter, Select = R-Shift",
    ),
    BROWSER_WASD(
        title = "Браузер / Онлайн-игры (WASD + J/K)",
        description = "W/A/S/D, A = J, B = K, Start = Enter, Select = Пробел",
    ),
    TV_NAVIGATION(
        title = "Навигация ТВ (Enter / Back)",
        description = "Стрелки, A = Enter, B = Esc/Back, Start = Пробел, Select = Tab",
    ),
}

enum class DendySkin(val title: String) {
    DENDY_CLASSIC("Dendy Classic"),
    DENDY_JUNIOR("Dendy Junior"),
    NES_RETRO("NES 1985"),
    FAMICOM_GOLD("Famicom Gold"),
}

enum class TurboRate(val hz: Int, val label: String) {
    HZ_10(10, "10 Гц"),
    HZ_15(15, "15 Гц"),
    HZ_20(20, "20 Гц"),
    HZ_30(30, "30 Гц"),
}

sealed interface ConnectionState {
    data object Idle : ConnectionState
    data object PermissionRequired : ConnectionState
    data object BluetoothDisabled : ConnectionState
    data object Registering : ConnectionState
    data object ReadyToPair : ConnectionState
    data class Connecting(val deviceName: String) : ConnectionState
    data class Connected(val deviceName: String, val deviceAddress: String) : ConnectionState
    data class UnsupportedApi(val message: String) : ConnectionState
    data class Error(val message: String) : ConnectionState
}

sealed interface WifiConnectionState {
    data object Disconnected : WifiConnectionState
    data object NoWifi : WifiConnectionState
    data class PairingWithCode(val targetLabel: String) : WifiConnectionState
    data class Connecting(val targetLabel: String) : WifiConnectionState
    data class AwaitingTvApproval(val tvIp: String) : WifiConnectionState
    data class PairedReadyToConnect(val tvIp: String, val message: String) : WifiConnectionState
    data class Connected(
        val targetLabel: String,
        val protocol: WifiProtocol,
        val detail: String = "",
    ) : WifiConnectionState
    data class Error(val message: String) : WifiConnectionState
}

data class DiscoveredTvUi(
    val name: String,
    val ip: String,
    val port: Int,
    val serviceType: String,
) {
    val isAdbPairing: Boolean get() = serviceType.contains("_adb-tls-pairing", ignoreCase = true)
    val isAdbConnect: Boolean get() = serviceType.contains("_adb-tls-connect", ignoreCase = true)
}

data class PairedDeviceUi(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val isLikelyTvOrPc: Boolean,
)

data class GamepadSettings(
    val transportMode: TransportMode = TransportMode.BLUETOOTH_HID,
    val wifiProtocol: WifiProtocol = WifiProtocol.ADB_PAIRING_CODE,
    val wifiTargetIp: String = "",
    val wifiPort: Int = 55400,
    val wifiPairPort: Int = 0,
    val wifiConnectPort: Int = 0,
    val wifiPlayerIndex: Int = 0,
    val compatibilityProfile: CompatibilityProfile = CompatibilityProfile.UNIVERSAL_TV,
    val keyboardPreset: KeyboardKeyPreset = KeyboardKeyPreset.RETROARCH_DEFAULT,
    val skin: DendySkin = DendySkin.DENDY_CLASSIC,
    val turboRate: TurboRate = TurboRate.HZ_15,
    val hapticEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val swapAB: Boolean = false,
    val eightWayDpad: Boolean = true,
    val keepScreenOn: Boolean = true,
    val lastConnectedAddress: String? = null,
)
