package com.example

import com.example.hid.CompatibilityProfile
import com.example.hid.HidDescriptors
import com.example.hid.KeyboardKeyPreset
import com.example.hid.NesButton
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HidDescriptorsTest {

    @Test
    fun `neutral gamepad report sends center hat and zero axes`() {
        val bytes = HidDescriptors.buildGamepadReport(
            pressed = emptySet(),
            turboOnPhase = false,
            swapAB = false,
            includeHatSwitch = true,
        )
        assertArrayEquals(byteArrayOf(0x08, 0x00, 0x00, 0x00, 0x00), bytes)
    }

    @Test
    fun `up right diagonal sets hat NE and positive X negative Y`() {
        val bytes = HidDescriptors.buildGamepadReport(
            pressed = setOf(NesButton.UP, NesButton.RIGHT, NesButton.A, NesButton.START),
            turboOnPhase = false,
            swapAB = false,
            includeHatSwitch = true,
        )
        // hat = 1 (NE), x = 127, y = -127, btn A (bit 0) + START (bit 11 -> 0x08 in high byte)
        assertEquals(0x01.toByte(), bytes[0])
        assertEquals(127.toByte(), bytes[1])
        assertEquals((-127).toByte(), bytes[2])
        assertEquals(0x01.toByte(), bytes[3])
        assertEquals(0x08.toByte(), bytes[4])
    }

    @Test
    fun `swap AB swaps button 1 and button 2`() {
        val normalA = HidDescriptors.buildGamepadReport(
            pressed = setOf(NesButton.A),
            turboOnPhase = false,
            swapAB = false,
            includeHatSwitch = false,
        )
        val swappedA = HidDescriptors.buildGamepadReport(
            pressed = setOf(NesButton.A),
            turboOnPhase = false,
            swapAB = true,
            includeHatSwitch = false,
        )
        assertEquals(0x01.toByte(), normalA[2])
        assertEquals(0x02.toByte(), swappedA[2])
    }

    @Test
    fun `turbo A pulses A button only when turboOnPhase is true`() {
        val onPhase = HidDescriptors.buildGamepadReport(
            pressed = setOf(NesButton.TURBO_A),
            turboOnPhase = true,
            swapAB = false,
            includeHatSwitch = false,
        )
        val offPhase = HidDescriptors.buildGamepadReport(
            pressed = setOf(NesButton.TURBO_A),
            turboOnPhase = false,
            swapAB = false,
            includeHatSwitch = false,
        )
        assertTrue((onPhase[2].toInt() and 0x01) != 0)
        assertTrue((offPhase[2].toInt() and 0x01) == 0)
    }

    @Test
    fun `keyboard retroarch preset maps arrows Z X Enter and RShift`() {
        val kb = HidDescriptors.buildKeyboardReport(
            pressed = setOf(NesButton.LEFT, NesButton.A, NesButton.SELECT),
            turboOnPhase = false,
            swapAB = false,
            preset = KeyboardKeyPreset.RETROARCH_DEFAULT,
        )
        assertEquals(0x20.toByte(), kb[0]) // Right Shift modifier
        assertEquals(0x50.toByte(), kb[2]) // Left Arrow
        assertEquals(0x1D.toByte(), kb[3]) // Z
    }

    @Test
    fun `all compatibility descriptors are non-empty`() {
        CompatibilityProfile.entries.forEach { profile ->
            assertTrue(HidDescriptors.descriptorFor(profile).isNotEmpty())
        }
    }
}
