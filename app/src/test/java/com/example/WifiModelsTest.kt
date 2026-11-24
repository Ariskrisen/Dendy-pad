package com.example

import com.example.hid.DiscoveredTvUi
import com.example.hid.GamepadSettings
import com.example.hid.WifiProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WifiModelsTest {

    @Test
    fun `adb pairing code protocol has expected defaults`() {
        val settings = GamepadSettings()
        assertEquals(WifiProtocol.ADB_PAIRING_CODE, settings.wifiProtocol)
        assertEquals(0, settings.wifiPairPort)
        assertEquals(0, settings.wifiConnectPort)
    }

    @Test
    fun `discovered tv flags distinguish pairing and connect adb services`() {
        val pairingService = DiscoveredTvUi(
            name = "adb-1234",
            ip = "192.168.1.50",
            port = 37123,
            serviceType = "_adb-tls-pairing._tcp.",
        )
        val connectService = DiscoveredTvUi(
            name = "adb-1234",
            ip = "192.168.1.50",
            port = 41234,
            serviceType = "_adb-tls-connect._tcp.",
        )

        assertTrue(pairingService.isAdbPairing)
        assertFalse(pairingService.isAdbConnect)

        assertFalse(connectService.isAdbPairing)
        assertTrue(connectService.isAdbConnect)
    }
}
