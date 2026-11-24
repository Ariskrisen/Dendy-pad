package com.example.wifi

import android.content.Context
import android.os.Build
import android.view.KeyEvent
import com.example.hid.NesButton
import com.example.hid.TvAction
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.AdbStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.OutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Security
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import javax.security.auth.x500.X500Principal
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.conscrypt.Conscrypt

class AdbWifiClient(private val context: Context) {

    companion object {
        init {
            try {
                if (Security.getProvider("Conscrypt") == null) {
                    Security.insertProviderAt(Conscrypt.newProvider(), 1)
                }
            } catch (_: Throwable) {
            }
        }
    }

    private class DendyAdbManager(
        private val privKey: PrivateKey,
        private val cert: Certificate,
    ) : AbsAdbConnectionManager() {

        init {
            api = Build.VERSION.SDK_INT
        }

        override fun getPrivateKey(): PrivateKey = privKey

        override fun getCertificate(): Certificate = cert

        override fun getDeviceName(): String = "DendyPad"
    }

    private val ioExecutor = Executors.newSingleThreadExecutor()
    private var manager: DendyAdbManager? = null
    private var shellStream: AdbStream? = null
    private var shellOutput: OutputStream? = null
    private val isConnected = AtomicBoolean(false)
    private var prevButtons: Set<NesButton> = emptySet()

    @Synchronized
    private fun ensureManager(): DendyAdbManager {
        manager?.let { return it }
        val (priv, cert) = loadOrGenerateCredentials()
        return DendyAdbManager(priv, cert).also { manager = it }
    }

    fun pairAndConnect(
        ip: String,
        pairPort: Int,
        pairingCode: String,
        connectPort: Int?,
        onPairingStarted: () -> Unit,
        onPairedConnecting: () -> Unit,
        onPairedNeedConnectPort: (String) -> Unit,
        onConnected: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        disconnect()
        ioExecutor.execute {
            try {
                val mgr = ensureManager()
                onPairingStarted()
                val paired = mgr.pair(ip.trim(), pairPort, pairingCode.trim())
                if (!paired) {
                    onError("Неверный код или порт подключения. Проверьте 6-значный код на экране ТВ.")
                    return@execute
                }

                onPairedConnecting()
                val connected = if (connectPort != null && connectPort > 0) {
                    mgr.connect(ip.trim(), connectPort)
                } else {
                    try {
                        mgr.connectTls(context, 5000)
                    } catch (_: Throwable) {
                        false
                    }
                }

                if (connected) {
                    openInteractiveShell(mgr)
                    isConnected.set(true)
                    val portLabel = if (connectPort != null && connectPort > 0) ":$connectPort" else " (TLS)"
                    onConnected("${ip.trim()}$portLabel")
                } else {
                    onPairedNeedConnectPort(
                        "Код принят! Теперь укажите «IP-адрес и порт» с главного экрана «Отладка по Wi-Fi» на ТВ и нажмите «Подключиться»."
                    )
                }
            } catch (e: Throwable) {
                isConnected.set(false)
                onError(humanizeAdbError(e))
            }
        }
    }

    fun connectDirect(
        ip: String,
        port: Int,
        onConnected: (String) -> Unit,
        onError: (String) -> Unit,
    ) {
        disconnect()
        ioExecutor.execute {
            try {
                val mgr = ensureManager()
                val ok = mgr.connect(ip.trim(), port)
                if (!ok) {
                    onError("Не удалось подключиться к $ip:$port. Если это Android TV 11+, сначала привяжите устройство по 6-значному коду.")
                    return@execute
                }
                openInteractiveShell(mgr)
                isConnected.set(true)
                onConnected("${ip.trim()}:$port")
            } catch (e: Throwable) {
                isConnected.set(false)
                onError(humanizeAdbError(e))
            }
        }
    }

    private fun openInteractiveShell(mgr: DendyAdbManager) {
        val stream = mgr.openStream("shell:")
        shellStream = stream
        shellOutput = stream.openOutputStream()

        // Drain stdout in background so ADB stream window never blocks
        val input = stream.openInputStream()
        Thread {
            try {
                val buf = ByteArray(1024)
                while (isConnected.get() && input.read(buf) != -1) {
                    // Discard shell echo
                }
            } catch (_: Throwable) {
            }
        }.apply {
            isDaemon = true
            start()
        }
    }

    fun sendGamepadState(pressed: Set<NesButton>, swapAB: Boolean) {
        if (!isConnected.get()) return
        val newlyPressed = pressed - prevButtons
        prevButtons = pressed
        if (newlyPressed.isEmpty()) return

        val keyCodes = newlyPressed.map { btn ->
            when (btn) {
                NesButton.UP -> KeyEvent.KEYCODE_DPAD_UP
                NesButton.DOWN -> KeyEvent.KEYCODE_DPAD_DOWN
                NesButton.LEFT -> KeyEvent.KEYCODE_DPAD_LEFT
                NesButton.RIGHT -> KeyEvent.KEYCODE_DPAD_RIGHT
                NesButton.A, NesButton.TURBO_A ->
                    if (swapAB) KeyEvent.KEYCODE_BACK else KeyEvent.KEYCODE_DPAD_CENTER
                NesButton.B, NesButton.TURBO_B ->
                    if (swapAB) KeyEvent.KEYCODE_DPAD_CENTER else KeyEvent.KEYCODE_BACK
                NesButton.START -> KeyEvent.KEYCODE_ENTER
                NesButton.SELECT -> KeyEvent.KEYCODE_MENU
            }
        }
        sendShellKeyEvents(keyCodes)
    }

    fun sendTvAction(action: TvAction) {
        if (!isConnected.get()) return
        val code = when (action) {
            TvAction.HOME -> KeyEvent.KEYCODE_HOME
            TvAction.BACK -> KeyEvent.KEYCODE_BACK
            TvAction.MENU -> KeyEvent.KEYCODE_MENU
            TvAction.VOL_UP -> KeyEvent.KEYCODE_VOLUME_UP
            TvAction.VOL_DOWN -> KeyEvent.KEYCODE_VOLUME_DOWN
            TvAction.MUTE -> KeyEvent.KEYCODE_VOLUME_MUTE
            TvAction.PLAY_PAUSE -> KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
        }
        sendShellKeyEvents(listOf(code))
    }

    private fun sendShellKeyEvents(codes: List<Int>) {
        val out = shellOutput ?: return
        val cmd = codes.joinToString(" && ", postfix = "\n") { "input keyevent $it" }
        ioExecutor.execute {
            try {
                synchronized(out) {
                    out.write(cmd.toByteArray(Charsets.UTF_8))
                    out.flush()
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun loadOrGenerateCredentials(): Pair<PrivateKey, Certificate> {
        val privFile = File(context.filesDir, "dendy_adb_priv_v2.der")
        val certFile = File(context.filesDir, "dendy_adb_cert_v2.der")
        if (privFile.exists() && certFile.exists()) {
            try {
                val kf = KeyFactory.getInstance("RSA")
                val priv = kf.generatePrivate(PKCS8EncodedKeySpec(privFile.readBytes()))
                val cf = CertificateFactory.getInstance("X.509")
                val cert = cf.generateCertificate(ByteArrayInputStream(certFile.readBytes()))
                return priv to cert
            } catch (_: Throwable) {
            }
        }

        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        val pair: KeyPair = kpg.generateKeyPair()

        val now = System.currentTimeMillis()
        val notBefore = Date(now - 86_400_000L)
        val notAfter = Date(now + 10L * 365L * 86_400_000L)
        val subject = X500Principal("CN=DendyPad")
        val builder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(now),
            notBefore,
            notAfter,
            subject,
            pair.public,
        )
        val signer = JcaContentSignerBuilder("SHA256withRSA").build(pair.private)
        val cert: X509Certificate = JcaX509CertificateConverter().getCertificate(builder.build(signer))

        privFile.writeBytes(pair.private.encoded)
        certFile.writeBytes(cert.encoded)
        return pair.private to cert
    }

    private fun humanizeAdbError(e: Throwable): String {
        val msg = e.localizedMessage ?: e.javaClass.simpleName
        return when {
            msg.contains("Pairing", ignoreCase = true) ->
                "Требуется привязка по коду Wi-Fi или подтверждение на экране ТВ."
            msg.contains("ECONNREFUSED", ignoreCase = true) || msg.contains("refused", ignoreCase = true) ->
                "Порт отклонён. Убедитесь, что «Отладка по Wi-Fi» на ТВ открыта и порт введён верно."
            msg.contains("timeout", ignoreCase = true) ->
                "Тайм-аут соединения. Проверьте IP-адрес и что телефон и ТВ в одной Wi-Fi сети."
            else -> msg
        }
    }

    fun disconnect() {
        isConnected.set(false)
        prevButtons = emptySet()
        try {
            shellStream?.close()
        } catch (_: Throwable) {
        }
        shellStream = null
        shellOutput = null
        try {
            manager?.disconnect()
        } catch (_: Throwable) {
        }
    }
}
