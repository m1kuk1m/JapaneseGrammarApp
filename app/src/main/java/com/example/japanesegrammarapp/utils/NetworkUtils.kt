package com.example.japanesegrammarapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.NetworkInterface

object NetworkUtils {

    fun getLocalIpAddress(context: Context? = null): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return "127.0.0.1"
            val ipList = mutableListOf<String>()
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress ?: continue
                        val name = intf.name.lowercase()
                        // Prioritize wireless LAN or access point (hotspot) interfaces
                        if (name.contains("wlan") || name.contains("ap") || name.contains("rndis") || name.contains("swlan")) {
                            return hostAddress
                        }
                        ipList.add(hostAddress)
                    }
                }
            }
            if (ipList.isNotEmpty()) {
                return ipList.first()
            }
        } catch (e: Exception) {
            AppLogger.e("NETWORK", "Error resolving local IP address", e)
        }
        return "127.0.0.1"
    }

    fun isWifiOrHotspotConnected(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        } catch (e: Exception) {
            return false
        }
    }
}
