package com.example.hid

import android.bluetooth.BluetoothHidDevice
import android.os.Build

object HidDescriptors {

    const val REPORT_ID_GAMEPAD = 1
    const val REPORT_ID_KEYBOARD = 2
    const val REPORT_ID_CONSUMER = 3

    // Report ID 1: Universal Gamepad (Hat Switch 4-bit + 4-bit pad + X/Y -127..127 + 16 buttons) = 5 bytes
    private val GAMEPAD_HAT_XY_BYTES = byteArrayOf(
        0x05, 0x01,       // Usage Page (Generic Desktop)
        0x09, 0x05,       // Usage (Game Pad)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), REPORT_ID_GAMEPAD.toByte(), // Report ID (1)
        // Hat switch (0..7, null = 8)
        0x05, 0x01,       //   Usage Page (Generic Desktop)
        0x09, 0x39,       //   Usage (Hat switch)
        0x15, 0x00,       //   Logical Minimum (0)
        0x25, 0x07,       //   Logical Maximum (7)
        0x35, 0x00,       //   Physical Minimum (0)
        0x46, 0x3B, 0x01, //   Physical Maximum (315)
        0x65, 0x14,       //   Unit (Eng Rot: Angular Pos)
        0x75, 0x04,       //   Report Size (4)
        0x95.toByte(), 0x01, // Report Count (1)
        0x81.toByte(), 0x42, // Input (Data, Var, Abs, Null State)
        // 4-bit padding
        0x65, 0x00,       //   Unit (None)
        0x75, 0x04,       //   Report Size (4)
        0x95.toByte(), 0x01, // Report Count (1)
        0x81.toByte(), 0x03, // Input (Const, Var, Abs)
        // X, Y axes (-127..127)
        0x05, 0x01,       //   Usage Page (Generic Desktop)
        0x09, 0x30,       //   Usage (X)
        0x09, 0x31,       //   Usage (Y)
        0x15, 0x81.toByte(), // Logical Minimum (-127)
        0x25, 0x7F,       //   Logical Maximum (127)
        0x75, 0x08,       //   Report Size (8)
        0x95.toByte(), 0x02, // Report Count (2)
        0x81.toByte(), 0x02, // Input (Data, Var, Abs)
        // 16 Buttons (Button 1..16)
        0x05, 0x09,       //   Usage Page (Button)
        0x19, 0x01,       //   Usage Minimum (Button 1)
        0x29, 0x10,       //   Usage Maximum (Button 16)
        0x15, 0x00,       //   Logical Minimum (0)
        0x25, 0x01,       //   Logical Maximum (1)
        0x75, 0x01,       //   Report Size (1)
        0x95.toByte(), 0x10, // Report Count (16)
        0x81.toByte(), 0x02, // Input (Data, Var, Abs)
        0xC0.toByte(),    // End Collection
    )

    // Report ID 1: Pure X/Y Gamepad (no Hat Switch, X/Y -127..127 + 16 buttons) = 4 bytes
    private val GAMEPAD_PURE_XY_BYTES = byteArrayOf(
        0x05, 0x01,       // Usage Page (Generic Desktop)
        0x09, 0x05,       // Usage (Game Pad)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), REPORT_ID_GAMEPAD.toByte(), // Report ID (1)
        0x05, 0x01,       //   Usage Page (Generic Desktop)
        0x09, 0x30,       //   Usage (X)
        0x09, 0x31,       //   Usage (Y)
        0x15, 0x81.toByte(), // Logical Minimum (-127)
        0x25, 0x7F,       //   Logical Maximum (127)
        0x75, 0x08,       //   Report Size (8)
        0x95.toByte(), 0x02, // Report Count (2)
        0x81.toByte(), 0x02, // Input (Data, Var, Abs)
        0x05, 0x09,       //   Usage Page (Button)
        0x19, 0x01,       //   Usage Minimum (Button 1)
        0x29, 0x10,       //   Usage Maximum (Button 16)
        0x15, 0x00,       //   Logical Minimum (0)
        0x25, 0x01,       //   Logical Maximum (1)
        0x75, 0x01,       //   Report Size (1)
        0x95.toByte(), 0x10, // Report Count (16)
        0x81.toByte(), 0x02, // Input (Data, Var, Abs)
        0xC0.toByte(),    // End Collection
    )

    // Report ID 2: Standard 8-byte HID Keyboard
    private val KEYBOARD_BYTES = byteArrayOf(
        0x05, 0x01,       // Usage Page (Generic Desktop)
        0x09, 0x06,       // Usage (Keyboard)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD.toByte(), // Report ID (2)
        // Modifier byte
        0x05, 0x07,       //   Usage Page (Keyboard/Keypad)
        0x19, 0xE0.toByte(), // Usage Minimum (Left Control)
        0x29, 0xE7.toByte(), // Usage Maximum (Right GUI)
        0x15, 0x00,       //   Logical Minimum (0)
        0x25, 0x01,       //   Logical Maximum (1)
        0x75, 0x01,       //   Report Size (1)
        0x95.toByte(), 0x08, // Report Count (8)
        0x81.toByte(), 0x02, // Input (Data, Var, Abs)
        // Reserved byte
        0x75, 0x08,       //   Report Size (8)
        0x95.toByte(), 0x01, // Report Count (1)
        0x81.toByte(), 0x03, // Input (Const, Var, Abs)
        // 6 Keycodes
        0x15, 0x00,       //   Logical Minimum (0)
        0x25, 0x65,       //   Logical Maximum (101)
        0x05, 0x07,       //   Usage Page (Keyboard/Keypad)
        0x19, 0x00,       //   Usage Minimum (0)
        0x29, 0x65,       //   Usage Maximum (101)
        0x75, 0x08,       //   Report Size (8)
        0x95.toByte(), 0x06, // Report Count (6)
        0x81.toByte(), 0x00, // Input (Data, Array)
        0xC0.toByte(),    // End Collection
    )

    // Report ID 3: Consumer Control (TV Home, Back, Volume, Play/Pause) = 2 bytes
    private val CONSUMER_BYTES = byteArrayOf(
        0x05, 0x0C,       // Usage Page (Consumer)
        0x09, 0x01,       // Usage (Consumer Control)
        0xA1.toByte(), 0x01, // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER.toByte(), // Report ID (3)
        0x15, 0x00,       //   Logical Minimum (0)
        0x26, 0xFF.toByte(), 0x03, // Logical Maximum (1023)
        0x19, 0x00,       //   Usage Minimum (0)
        0x2A, 0xFF.toByte(), 0x03, // Usage Maximum (1023)
        0x75, 0x10,       //   Report Size (16)
        0x95.toByte(), 0x01, // Report Count (1)
        0x81.toByte(), 0x00, // Input (Data, Array)
        0xC0.toByte(),    // End Collection
    )

    fun descriptorFor(profile: CompatibilityProfile): ByteArray = when (profile) {
        CompatibilityProfile.UNIVERSAL_TV -> GAMEPAD_HAT_XY_BYTES + CONSUMER_BYTES
        CompatibilityProfile.DUAL_HYBRID -> GAMEPAD_HAT_XY_BYTES + KEYBOARD_BYTES + CONSUMER_BYTES
        CompatibilityProfile.PURE_GAMEPAD_XY -> GAMEPAD_PURE_XY_BYTES + CONSUMER_BYTES
        CompatibilityProfile.SMART_TV_KEYBOARD -> KEYBOARD_BYTES + CONSUMER_BYTES
    }

    fun subclassFor(profile: CompatibilityProfile): Byte {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return 0
        return when (profile) {
            CompatibilityProfile.UNIVERSAL_TV,
            CompatibilityProfile.PURE_GAMEPAD_XY,
            -> BluetoothHidDevice.SUBCLASS2_GAMEPAD

            CompatibilityProfile.DUAL_HYBRID -> BluetoothHidDevice.SUBCLASS1_COMBO
            CompatibilityProfile.SMART_TV_KEYBOARD -> BluetoothHidDevice.SUBCLASS1_KEYBOARD
        }
    }

    // Build Report ID 1 bytes from active buttons
    fun buildGamepadReport(
        pressed: Set<NesButton>,
        turboOnPhase: Boolean,
        swapAB: Boolean,
        includeHatSwitch: Boolean,
    ): ByteArray {
        val up = NesButton.UP in pressed
        val down = NesButton.DOWN in pressed
        val left = NesButton.LEFT in pressed
        val right = NesButton.RIGHT in pressed

        val x: Byte = when {
            left && !right -> (-127).toByte()
            right && !left -> 127.toByte()
            else -> 0
        }
        val y: Byte = when {
            up && !down -> (-127).toByte()
            down && !up -> 127.toByte()
            else -> 0
        }

        // Hat switch: 0=N, 1=NE, 2=E, 3=SE, 4=S, 5=SW, 6=W, 7=NW, 8=Center/Null
        val hat: Int = when {
            up && right && !down && !left -> 1
            down && right && !up && !left -> 3
            down && left && !up && !right -> 5
            up && left && !down && !right -> 7
            up && !down -> 0
            right && !left -> 2
            down && !up -> 4
            left && !right -> 6
            else -> 8
        }

        val physicalA = NesButton.A in pressed || (NesButton.TURBO_A in pressed && turboOnPhase)
        val physicalB = NesButton.B in pressed || (NesButton.TURBO_B in pressed && turboOnPhase)

        val btnA = if (swapAB) physicalB else physicalA
        val btnB = if (swapAB) physicalA else physicalB
        val btnTurboX = NesButton.TURBO_A in pressed
        val btnTurboY = NesButton.TURBO_B in pressed
        val select = NesButton.SELECT in pressed
        val start = NesButton.START in pressed

        // Linux hid-input.c Gamepad Button bits (0..15):
        // bit 0 (btn 1): BTN_SOUTH / A
        // bit 1 (btn 2): BTN_EAST / B
        // bit 3 (btn 4): BTN_NORTH / X
        // bit 4 (btn 5): BTN_WEST / Y
        // bit 10 (btn 11): BTN_SELECT
        // bit 11 (btn 12): BTN_START
        var btnMask = 0
        if (btnA) btnMask = btnMask or (1 shl 0)
        if (btnB) btnMask = btnMask or (1 shl 1)
        if (btnTurboX) btnMask = btnMask or (1 shl 3)
        if (btnTurboY) btnMask = btnMask or (1 shl 4)
        if (select) btnMask = btnMask or (1 shl 10)
        if (start) btnMask = btnMask or (1 shl 11)

        val btnLow = (btnMask and 0xFF).toByte()
        val btnHigh = ((btnMask shr 8) and 0xFF).toByte()

        return if (includeHatSwitch) {
            byteArrayOf((hat and 0x0F).toByte(), x, y, btnLow, btnHigh)
        } else {
            byteArrayOf(x, y, btnLow, btnHigh)
        }
    }

    // Build Report ID 2 (8-byte HID Keyboard)
    fun buildKeyboardReport(
        pressed: Set<NesButton>,
        turboOnPhase: Boolean,
        swapAB: Boolean,
        preset: KeyboardKeyPreset,
    ): ByteArray {
        var modifiers = 0
        val keys = ArrayList<Byte>(6)

        fun addKey(hidUsage: Int) {
            if (keys.size < 6) keys.add(hidUsage.toByte())
        }

        val physicalA = NesButton.A in pressed || (NesButton.TURBO_A in pressed && turboOnPhase)
        val physicalB = NesButton.B in pressed || (NesButton.TURBO_B in pressed && turboOnPhase)
        val btnA = if (swapAB) physicalB else physicalA
        val btnB = if (swapAB) physicalA else physicalB

        when (preset) {
            KeyboardKeyPreset.RETROARCH_DEFAULT -> {
                if (NesButton.UP in pressed) addKey(0x52)    // Up Arrow
                if (NesButton.DOWN in pressed) addKey(0x51)  // Down Arrow
                if (NesButton.LEFT in pressed) addKey(0x50)  // Left Arrow
                if (NesButton.RIGHT in pressed) addKey(0x4F) // Right Arrow
                if (btnA) addKey(0x1D)                       // Z
                if (btnB) addKey(0x1B)                       // X
                if (NesButton.START in pressed) addKey(0x28) // Enter
                if (NesButton.SELECT in pressed) modifiers = modifiers or 0x20 // Right Shift
            }

            KeyboardKeyPreset.BROWSER_WASD -> {
                if (NesButton.UP in pressed) addKey(0x1A)    // W
                if (NesButton.DOWN in pressed) addKey(0x16)  // S
                if (NesButton.LEFT in pressed) addKey(0x04)  // A
                if (NesButton.RIGHT in pressed) addKey(0x07) // D
                if (btnA) addKey(0x0D)                       // J
                if (btnB) addKey(0x0E)                       // K
                if (NesButton.START in pressed) addKey(0x28) // Enter
                if (NesButton.SELECT in pressed) addKey(0x2C) // Space
            }

            KeyboardKeyPreset.TV_NAVIGATION -> {
                if (NesButton.UP in pressed) addKey(0x52)    // Up Arrow
                if (NesButton.DOWN in pressed) addKey(0x51)  // Down Arrow
                if (NesButton.LEFT in pressed) addKey(0x50)  // Left Arrow
                if (NesButton.RIGHT in pressed) addKey(0x4F) // Right Arrow
                if (btnA) addKey(0x28)                       // Enter
                if (btnB) addKey(0x29)                       // Escape / Back
                if (NesButton.START in pressed) addKey(0x2C) // Space
                if (NesButton.SELECT in pressed) addKey(0x2B) // Tab
            }
        }

        val report = ByteArray(8)
        report[0] = modifiers.toByte()
        report[1] = 0x00
        for (i in keys.indices) {
            report[2 + i] = keys[i]
        }
        return report
    }

    fun consumerUsageFor(action: TvAction): Int = when (action) {
        TvAction.HOME -> 0x0223       // AC Home
        TvAction.BACK -> 0x0224       // AC Back
        TvAction.MENU -> 0x0040       // Menu
        TvAction.VOL_UP -> 0x00E9     // Volume Increment
        TvAction.VOL_DOWN -> 0x00EA   // Volume Decrement
        TvAction.MUTE -> 0x00E2       // Mute
        TvAction.PLAY_PAUSE -> 0x00CD // Play/Pause
    }

    fun formatHex(reportId: Int, data: ByteArray): String {
        val hex = data.joinToString(" ") { "%02X".format(it) }
        return "ID:%02d [%s]".format(reportId, hex)
    }
}
