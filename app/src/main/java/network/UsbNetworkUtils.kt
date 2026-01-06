package com.usb.drivingremote.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.Intent
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetSocketAddress

import java.net.NetworkInterface
import java.net.Socket

fun isUsbTetheringEnabled(): Boolean {
    return try {
        NetworkInterface.getNetworkInterfaces().toList().any { iface ->
            iface.isUp &&
                    !iface.isLoopback &&
                    iface.name.contains("rndis", true)
        }
    } catch (e: Exception) {
        false
    }
}


fun openUsbTetheringSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}


