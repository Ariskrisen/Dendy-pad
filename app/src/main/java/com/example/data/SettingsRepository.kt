package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.hid.CompatibilityProfile
import com.example.hid.DendySkin
import com.example.hid.GamepadSettings
import com.example.hid.KeyboardKeyPreset
import com.example.hid.TransportMode
import com.example.hid.TurboRate
import com.example.hid.WifiProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "dendy_pad_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val TRANSPORT = stringPreferencesKey("transport_mode")
        val WIFI_PROTOCOL = stringPreferencesKey("wifi_protocol")
        val WIFI_IP = stringPreferencesKey("wifi_target_ip")
        val WIFI_PORT = intPreferencesKey("wifi_port")
        val WIFI_PAIR_PORT = intPreferencesKey("wifi_pair_port")
        val WIFI_CONNECT_PORT = intPreferencesKey("wifi_connect_port")
        val WIFI_PLAYER = intPreferencesKey("wifi_player_index")
        val PROFILE = stringPreferencesKey("compatibility_profile")
        val KEYBOARD_PRESET = stringPreferencesKey("keyboard_preset")
        val SKIN = stringPreferencesKey("dendy_skin")
        val TURBO_RATE = stringPreferencesKey("turbo_rate")
        val HAPTIC = booleanPreferencesKey("haptic_enabled")
        val SOUND = booleanPreferencesKey("sound_enabled")
        val SWAP_AB = booleanPreferencesKey("swap_ab")
        val EIGHT_WAY = booleanPreferencesKey("eight_way_dpad")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val LAST_ADDRESS = stringPreferencesKey("last_connected_address")
    }

    val settingsFlow: Flow<GamepadSettings> = context.dataStore.data.map { prefs ->
        decode(prefs)
    }

    suspend fun update(transform: (GamepadSettings) -> GamepadSettings) {
        context.dataStore.edit { prefs ->
            val current = decode(prefs)
            val next = transform(current)
            prefs[Keys.TRANSPORT] = next.transportMode.name
            prefs[Keys.WIFI_PROTOCOL] = next.wifiProtocol.name
            prefs[Keys.WIFI_IP] = next.wifiTargetIp
            prefs[Keys.WIFI_PORT] = next.wifiPort
            prefs[Keys.WIFI_PAIR_PORT] = next.wifiPairPort
            prefs[Keys.WIFI_CONNECT_PORT] = next.wifiConnectPort
            prefs[Keys.WIFI_PLAYER] = next.wifiPlayerIndex
            prefs[Keys.PROFILE] = next.compatibilityProfile.name
            prefs[Keys.KEYBOARD_PRESET] = next.keyboardPreset.name
            prefs[Keys.SKIN] = next.skin.name
            prefs[Keys.TURBO_RATE] = next.turboRate.name
            prefs[Keys.HAPTIC] = next.hapticEnabled
            prefs[Keys.SOUND] = next.soundEnabled
            prefs[Keys.SWAP_AB] = next.swapAB
            prefs[Keys.EIGHT_WAY] = next.eightWayDpad
            prefs[Keys.KEEP_SCREEN_ON] = next.keepScreenOn
            if (next.lastConnectedAddress != null) {
                prefs[Keys.LAST_ADDRESS] = next.lastConnectedAddress
            } else {
                prefs.remove(Keys.LAST_ADDRESS)
            }
        }
    }

    private fun decode(prefs: Preferences): GamepadSettings {
        val wifiProtocol = prefs[Keys.WIFI_PROTOCOL]?.let { n ->
            WifiProtocol.entries.firstOrNull { it.name == n }
        } ?: WifiProtocol.ADB_PAIRING_CODE
        return GamepadSettings(
            transportMode = prefs[Keys.TRANSPORT]?.let { n ->
                TransportMode.entries.firstOrNull { it.name == n }
            } ?: TransportMode.BLUETOOTH_HID,
            wifiProtocol = wifiProtocol,
            wifiTargetIp = prefs[Keys.WIFI_IP] ?: "",
            wifiPort = prefs[Keys.WIFI_PORT] ?: wifiProtocol.defaultPort,
            wifiPairPort = prefs[Keys.WIFI_PAIR_PORT] ?: 0,
            wifiConnectPort = prefs[Keys.WIFI_CONNECT_PORT] ?: 0,
            wifiPlayerIndex = (prefs[Keys.WIFI_PLAYER] ?: 0).coerceIn(0, 3),
            compatibilityProfile = prefs[Keys.PROFILE]?.let { n ->
                CompatibilityProfile.entries.firstOrNull { it.name == n }
            } ?: CompatibilityProfile.UNIVERSAL_TV,
            keyboardPreset = prefs[Keys.KEYBOARD_PRESET]?.let { n ->
                KeyboardKeyPreset.entries.firstOrNull { it.name == n }
            } ?: KeyboardKeyPreset.RETROARCH_DEFAULT,
            skin = prefs[Keys.SKIN]?.let { n ->
                DendySkin.entries.firstOrNull { it.name == n }
            } ?: DendySkin.DENDY_CLASSIC,
            turboRate = prefs[Keys.TURBO_RATE]?.let { n ->
                TurboRate.entries.firstOrNull { it.name == n }
            } ?: TurboRate.HZ_15,
            hapticEnabled = prefs[Keys.HAPTIC] ?: true,
            soundEnabled = prefs[Keys.SOUND] ?: true,
            swapAB = prefs[Keys.SWAP_AB] ?: false,
            eightWayDpad = prefs[Keys.EIGHT_WAY] ?: true,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            lastConnectedAddress = prefs[Keys.LAST_ADDRESS],
        )
    }
}
