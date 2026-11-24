package com.example.wifi

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import com.example.hid.DiscoveredTvUi
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WifiTvDiscovery(private val context: Context) {

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val foundMap = ConcurrentHashMap<String, DiscoveredTvUi>()
    private val activeListeners = mutableListOf<NsdManager.DiscoveryListener>()
    private var multicastLock: WifiManager.MulticastLock? = null

    private val _discoveredTvs = MutableStateFlow<List<DiscoveredTvUi>>(emptyList())
    val discoveredTvs: StateFlow<List<DiscoveredTvUi>> = _discoveredTvs.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val serviceTypes = listOf(
        "_adb-tls-pairing._tcp.",
        "_adb-tls-connect._tcp.",
        "_androidtvremote2._tcp.",
        "_googlecast._tcp.",
        "_adb._tcp.",
    )

    fun isWifiConnected(): Boolean {
        val cm = connectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun getLocalIpv4Address(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "0.0.0.0"
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (addr is Inet4Address && !addr.isLoopbackAddress) {
                        val ip = addr.hostAddress ?: continue
                        if (!ip.startsWith("169.254.")) {
                            return ip
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
        return "0.0.0.0"
    }

    fun startDiscovery() {
        stopDiscovery()
        val nsd = nsdManager ?: return
        foundMap.clear()
        _discoveredTvs.value = emptyList()
        _isScanning.value = true

        try {
            multicastLock = wifiManager?.createMulticastLock("DendyPadNsdLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
        } catch (_: Exception) {
        }

        for (type in serviceTypes) {
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {}

                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    resolveService(nsd, serviceInfo)
                }

                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    foundMap.remove("${serviceInfo.serviceType}:${serviceInfo.serviceName}")
                    emitList()
                }

                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            }
            try {
                nsd.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
                activeListeners.add(listener)
            } catch (_: Exception) {
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveService(nsd: NsdManager, serviceInfo: NsdServiceInfo) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                nsd.registerServiceInfoCallback(
                    serviceInfo,
                    { it.run() },
                    object : NsdManager.ServiceInfoCallback {
                        override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {}
                        override fun onServiceUpdated(info: NsdServiceInfo) {
                            val ipv4 = info.hostAddresses
                                .filterIsInstance<Inet4Address>()
                                .firstOrNull()?.hostAddress ?: return
                            val key = "${info.serviceType}:${info.serviceName}"
                            foundMap[key] = DiscoveredTvUi(
                                name = cleanServiceName(info.serviceName, info.serviceType),
                                ip = ipv4,
                                port = info.port,
                                serviceType = info.serviceType,
                            )
                            emitList()
                            try {
                                nsd.unregisterServiceInfoCallback(this)
                            } catch (_: Exception) {
                            }
                        }

                        override fun onServiceLost() {}
                        override fun onServiceInfoCallbackUnregistered() {}
                    },
                )
            } else {
                nsd.resolveService(
                    serviceInfo,
                    object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {}
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host ?: return
                            val ip = (host as? Inet4Address)?.hostAddress ?: host.hostAddress ?: return
                            if (ip.contains(':')) return
                            val key = "${info.serviceType}:${info.serviceName}"
                            foundMap[key] = DiscoveredTvUi(
                                name = cleanServiceName(info.serviceName, info.serviceType),
                                ip = ip,
                                port = info.port,
                                serviceType = info.serviceType,
                            )
                            emitList()
                        }
                    },
                )
            }
        } catch (_: Exception) {
        }
    }

    private fun cleanServiceName(raw: String, serviceType: String): String {
        val cleaned = raw.replace(Regex("-[0-9a-fA-F]{16,}$"), "").trim().ifBlank { raw }
        return when {
            serviceType.contains("_adb-tls-pairing", ignoreCase = true) -> "Код Wi-Fi ($cleaned)"
            serviceType.contains("_adb-tls-connect", ignoreCase = true) -> "ADB TLS ($cleaned)"
            else -> cleaned
        }
    }

    private fun emitList() {
        _discoveredTvs.value = foundMap.values
            .distinctBy { "${it.ip}:${it.port}:${it.serviceType}" }
            .sortedWith(
                compareByDescending<DiscoveredTvUi> { it.isAdbPairing }
                    .thenByDescending { it.isAdbConnect }
                    .thenByDescending {
                        it.serviceType.contains("androidtv", ignoreCase = true) ||
                            it.serviceType.contains("googlecast", ignoreCase = true)
                    }
                    .thenBy { it.name }
            )
    }

    fun stopDiscovery() {
        val nsd = nsdManager
        if (nsd != null) {
            for (listener in activeListeners) {
                try {
                    nsd.stopServiceDiscovery(listener)
                } catch (_: Exception) {
                }
            }
        }
        activeListeners.clear()
        try {
            if (multicastLock?.isHeld == true) {
                multicastLock?.release()
            }
        } catch (_: Exception) {
        }
        multicastLock = null
        _isScanning.value = false
    }
}
