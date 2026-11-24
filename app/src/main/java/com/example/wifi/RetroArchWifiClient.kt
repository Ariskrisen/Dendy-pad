package com.example.wifi

import com.example.hid.NesButton
import com.example.hid.TvAction
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RetroArchWifiClient {

    companion object {
        const val DEFAULT_GAMEPAD_PORT = 55400
        const val DEFAULT_CMD_PORT = 55355

        // Libretro RETRO_DEVICE_JOYPAD = 1
        private const val RETRO_DEVICE_JOYPAD = 1

        // Libretro RETRO_DEVICE_ID_JOYPAD_*
        private const val ID_B = 0
        private const val ID_Y = 1
        private const val ID_SELECT = 2
        private const val ID_START = 3
        private const val ID_UP = 4
        private const val ID_DOWN = 5
        private const val ID_LEFT = 6
        private const val ID_RIGHT = 7
        private const val ID_A = 8
        private const val ID_X = 9
    }

    private var socket: DatagramSocket? = null
    private var prevButtonStates: Map<Int, Boolean> = emptyMap()

    @Synchronized
    private fun ensureSocket(): DatagramSocket {
        val existing = socket
        if (existing != null && !existing.isClosed) return existing
        return DatagramSocket().also { socket = it }
    }

    fun pingRetroArch(ip: String, timeoutMs: Int = 1200): Result<String> {
        return try {
            val addr = InetAddress.getByName(ip.trim())
            DatagramSocket().use { pingSocket ->
                pingSocket.soTimeout = timeoutMs
                val payload = "VERSION".toByteArray(Charsets.US_ASCII)
                pingSocket.send(DatagramPacket(payload, payload.size, addr, DEFAULT_CMD_PORT))
                val buf = ByteArray(256)
                val reply = DatagramPacket(buf, buf.size)
                pingSocket.receive(reply)
                val text = String(reply.data, 0, reply.length, Charsets.UTF_8).trim()
                Result.success(text.ifBlank { "RetroArch" })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun sendGamepadState(
        ip: String,
        basePort: Int,
        playerIndex: Int,
        pressed: Set<NesButton>,
        turboOnPhase: Boolean,
        swapAB: Boolean,
    ): String {
        val physicalA = NesButton.A in pressed || (NesButton.TURBO_A in pressed && turboOnPhase)
        val physicalB = NesButton.B in pressed || (NesButton.TURBO_B in pressed && turboOnPhase)

        val btnA = if (swapAB) physicalB else physicalA
        val btnB = if (swapAB) physicalA else physicalB

        val nextStates = mapOf(
            ID_UP to (NesButton.UP in pressed),
            ID_DOWN to (NesButton.DOWN in pressed),
            ID_LEFT to (NesButton.LEFT in pressed),
            ID_RIGHT to (NesButton.RIGHT in pressed),
            ID_SELECT to (NesButton.SELECT in pressed),
            ID_START to (NesButton.START in pressed),
            ID_A to btnA,
            ID_B to btnB,
            ID_X to (NesButton.TURBO_A in pressed),
            ID_Y to (NesButton.TURBO_B in pressed),
        )

        val targetPort = basePort + playerIndex
        var sentCount = 0
        try {
            val addr = InetAddress.getByName(ip.trim())
            val sock = ensureSocket()
            for ((id, isDown) in nextStates) {
                val wasDown = prevButtonStates[id] ?: false
                if (isDown != wasDown) {
                    val packetBytes = encodeRemoteMessage(
                        port = playerIndex,
                        device = RETRO_DEVICE_JOYPAD,
                        index = 0,
                        id = id,
                        state = if (isDown) 1 else 0,
                    )
                    sock.send(DatagramPacket(packetBytes, packetBytes.size, addr, targetPort))
                    sentCount++
                }
            }
            prevButtonStates = nextStates
        } catch (_: Exception) {
        }

        val activeNames = nextStates.filterValues { it }.keys.joinToString(",")
        return "UDP:$targetPort P${playerIndex + 1} [${if (activeNames.isEmpty()) "IDLE" else activeNames}] ($sentCount pkt)"
    }

    fun sendTvCommand(ip: String, action: TvAction): Boolean {
        val cmd = when (action) {
            TvAction.MENU, TvAction.HOME -> "MENU_TOGGLE"
            TvAction.BACK -> "PAUSE_TOGGLE"
            TvAction.PLAY_PAUSE -> "PAUSE_TOGGLE"
            TvAction.VOL_UP -> "VOLUME_UP"
            TvAction.VOL_DOWN -> "VOLUME_DOWN"
            TvAction.MUTE -> "MUTE_TOGGLE"
        }
        return try {
            val addr = InetAddress.getByName(ip.trim())
            val data = cmd.toByteArray(Charsets.US_ASCII)
            ensureSocket().send(DatagramPacket(data, data.size, addr, DEFAULT_CMD_PORT))
            true
        } catch (_: Exception) {
            false
        }
    }

    // Standard 20-byte network-byte-order RetroArch remote_message struct
    private fun encodeRemoteMessage(
        port: Int,
        device: Int,
        index: Int,
        id: Int,
        state: Int,
    ): ByteArray {
        val buf = ByteBuffer.allocate(20).order(ByteOrder.BIG_ENDIAN)
        buf.putInt(port)
        buf.putInt(device)
        buf.putInt(index)
        buf.putInt(id)
        buf.putShort(state.toShort())
        buf.putShort(0) // struct alignment tail padding
        return buf.array()
    }

    fun resetState() {
        prevButtonStates = emptyMap()
    }

    fun close() {
        prevButtonStates = emptyMap()
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }
}
