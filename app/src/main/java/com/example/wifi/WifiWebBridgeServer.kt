package com.example.wifi

import android.util.Base64
import com.example.hid.NesButton
import com.example.hid.TvAction
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiWebBridgeServer {

    companion object {
        const val HTTP_WS_PORT = 8765
        const val UDP_JSON_PORT = 55410
        private const val WS_MAGIC = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    }

    private val running = AtomicBoolean(false)
    private var serverSocket: ServerSocket? = null
    private var udpSocket: DatagramSocket? = null
    private val acceptExecutor = Executors.newSingleThreadExecutor()
    private val clientExecutor = Executors.newCachedThreadPool()
    private val wsClients = CopyOnWriteArrayList<OutputStream>()

    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients.asStateFlow()

    fun start(port: Int = HTTP_WS_PORT, onError: (String) -> Unit) {
        if (running.get()) return
        running.set(true)
        acceptExecutor.execute {
            try {
                val ss = ServerSocket(port)
                serverSocket = ss
                while (running.get()) {
                    val client = ss.accept()
                    clientExecutor.execute { handleClient(client) }
                }
            } catch (e: Exception) {
                if (running.get()) {
                    running.set(false)
                    onError(e.localizedMessage ?: "Не удалось запустить Wi-Fi сервер")
                }
            }
        }
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val out = socket.getOutputStream()
            val requestLine = reader.readLine() ?: return
            var wsKey: String? = null
            var isUpgrade = false

            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val colon = line.indexOf(':')
                if (colon > 0) {
                    val k = line.substring(0, colon).trim().lowercase()
                    val v = line.substring(colon + 1).trim()
                    if (k == "upgrade" && v.equals("websocket", ignoreCase = true)) {
                        isUpgrade = true
                    } else if (k == "sec-websocket-key") {
                        wsKey = v
                    }
                }
            }

            if (isUpgrade && wsKey != null) {
                val acceptHash = Base64.encodeToString(
                    MessageDigest.getInstance("SHA-1")
                        .digest((wsKey + WS_MAGIC).toByteArray(Charsets.US_ASCII)),
                    Base64.NO_WRAP,
                )
                val handshake = buildString {
                    append("HTTP/1.1 101 Switching Protocols\r\n")
                    append("Upgrade: websocket\r\n")
                    append("Connection: Upgrade\r\n")
                    append("Sec-WebSocket-Accept: $acceptHash\r\n\r\n")
                }
                out.write(handshake.toByteArray(Charsets.US_ASCII))
                out.flush()
                wsClients.add(out)
                _connectedClients.value = wsClients.size

                // Keep socket open until closed by browser
                val input = socket.getInputStream()
                val buf = ByteArray(256)
                while (running.get() && input.read(buf) != -1) {
                    // Wait for disconnect
                }
                wsClients.remove(out)
                _connectedClients.value = wsClients.size
                socket.close()
            } else {
                val html = receiverHtmlPage()
                val bytes = html.toByteArray(Charsets.UTF_8)
                val header = buildString {
                    append("HTTP/1.1 200 OK\r\n")
                    append("Content-Type: text/html; charset=utf-8\r\n")
                    append("Content-Length: ${bytes.size}\r\n")
                    append("Cache-Control: no-store\r\n")
                    append("Connection: close\r\n\r\n")
                }
                out.write(header.toByteArray(Charsets.US_ASCII))
                out.write(bytes)
                out.flush()
                socket.close()
            }
        } catch (_: Exception) {
            try {
                socket.close()
            } catch (_: Exception) {
            }
        }
    }

    fun broadcastState(
        pressed: Set<NesButton>,
        turboOnPhase: Boolean,
        swapAB: Boolean,
        targetIp: String?,
    ): String {
        val list = pressed.joinToString(",") { "\"${it.name}\"" }
        val json = """{"type":"pad","buttons":[$list],"turbo":$turboOnPhase,"swapAB":$swapAB}"""
        sendWebSocketText(json)

        if (!targetIp.isNullOrBlank()) {
            sendUdpJson(targetIp, json)
        }
        return "WS(${wsClients.size}) JSON [${pressed.joinToString("+") { it.name }.ifEmpty { "IDLE" }}]"
    }

    fun broadcastTvAction(action: TvAction, targetIp: String?) {
        val json = """{"type":"tv","action":"${action.name}"}"""
        sendWebSocketText(json)
        if (!targetIp.isNullOrBlank()) {
            sendUdpJson(targetIp, json)
        }
    }

    private fun sendUdpJson(ip: String, json: String) {
        clientExecutor.execute {
            try {
                val sock = udpSocket ?: DatagramSocket().also { udpSocket = it }
                val bytes = json.toByteArray(Charsets.UTF_8)
                sock.send(DatagramPacket(bytes, bytes.size, InetAddress.getByName(ip.trim()), UDP_JSON_PORT))
            } catch (_: Exception) {
            }
        }
    }

    private fun sendWebSocketText(text: String) {
        if (wsClients.isEmpty()) return
        val payload = text.toByteArray(Charsets.UTF_8)
        val frame = buildWsFrame(payload)
        for (client in wsClients) {
            try {
                synchronized(client) {
                    client.write(frame)
                    client.flush()
                }
            } catch (_: Exception) {
                wsClients.remove(client)
                _connectedClients.value = wsClients.size
            }
        }
    }

    private fun buildWsFrame(payload: ByteArray): ByteArray {
        val len = payload.size
        return when {
            len <= 125 -> {
                val out = ByteArray(2 + len)
                out[0] = 0x81.toByte()
                out[1] = len.toByte()
                System.arraycopy(payload, 0, out, 2, len)
                out
            }

            else -> {
                val out = ByteArray(4 + len)
                out[0] = 0x81.toByte()
                out[1] = 126.toByte()
                out[2] = ((len shr 8) and 0xFF).toByte()
                out[3] = (len and 0xFF).toByte()
                System.arraycopy(payload, 0, out, 4, len)
                out
            }
        }
    }

    fun stop() {
        running.set(false)
        wsClients.clear()
        _connectedClients.value = 0
        try {
            serverSocket?.close()
        } catch (_: Exception) {
        }
        serverSocket = null
        try {
            udpSocket?.close()
        } catch (_: Exception) {
        }
        udpSocket = null
    }

    private fun receiverHtmlPage(): String = """
        <!DOCTYPE html>
        <html lang="ru">
        <head>
        <meta charset="utf-8" />
        <meta name="viewport" content="width=device-width,initial-scale=1" />
        <title>Dendy Pad Wi-Fi TV Receiver</title>
        <style>
          body{margin:0;background:#151416;color:#f6f1e6;font-family:system-ui,sans-serif;display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh}
          .shell{background:#eae2d0;padding:18px;border-radius:26px;box-shadow:0 20px 60px rgba(0,0,0,.6)}
          .face{background:#343235;padding:24px 32px;border-radius:16px;display:flex;gap:36px;align-items:center}
          .badge{background:#b81d24;color:#f6f1e6;padding:6px 14px;border-radius:8px;font-weight:900;letter-spacing:2px;text-align:center}
          .grid{display:grid;grid-template-columns:repeat(3,48px);gap:6px}
          .key{height:48px;border-radius:10px;background:#222;display:flex;align-items:center;justify-content:center;font-weight:700;border:2px solid #47434b;transition:.06s}
          .on{background:#d9262e;color:#fff;border-color:#ffd166;transform:scale(.95);box-shadow:0 0 18px #d9262e}
          .ab{display:grid;grid-template-columns:repeat(2,72px);gap:12px}
          .btn{height:72px;border-radius:50%;background:#7a1217;display:flex;align-items:center;justify-content:center;font-weight:800;border:4px solid #eae2d0}
          .btn.on{background:#ff3b30;box-shadow:0 0 22px #ff3b30}
          .status{margin-top:18px;font-family:monospace;color:#39d353;font-size:15px}
        </style>
        </head>
        <body>
          <div class="shell"><div class="face">
            <div class="grid">
              <div></div><div id="UP" class="key">▲</div><div></div>
              <div id="LEFT" class="key">◀</div><div class="key" style="opacity:.3">●</div><div id="RIGHT" class="key">▶</div>
              <div></div><div id="DOWN" class="key">▼</div><div></div>
            </div>
            <div style="display:flex;flex-direction:column;gap:16px;align-items:center">
              <div class="badge">DENDY WI-FI</div>
              <div style="display:flex;gap:10px">
                <div id="SELECT" class="key" style="width:76px;height:30px">SELECT</div>
                <div id="START" class="key" style="width:76px;height:30px">START</div>
              </div>
            </div>
            <div class="ab">
              <div id="TURBO_B" class="btn">TB</div>
              <div id="TURBO_A" class="btn">TA</div>
              <div id="B" class="btn">B</div>
              <div id="A" class="btn">A</div>
            </div>
          </div></div>
          <div id="st" class="status">Подключение WebSocket...</div>
        <script>
          const keys=["UP","DOWN","LEFT","RIGHT","SELECT","START","B","A","TURBO_B","TURBO_A"];
          function connect(){
            const ws=new WebSocket("ws://"+location.host);
            ws.onopen=()=>document.getElementById("st").textContent="Dendy Pad подключён по Wi-Fi (Live)";
            ws.onmessage=(e)=>{
              const d=JSON.parse(e.data);
              if(d.type==="pad"){
                const s=new Set(d.buttons||[]);
                keys.forEach(k=>document.getElementById(k).classList.toggle("on",s.has(k)));
              }
            };
            ws.onclose=()=>{document.getElementById("st").textContent="Переподключение...";setTimeout(connect,1000);};
          }
          connect();
        </script>
        </body>
        </html>
    """.trimIndent()
}
