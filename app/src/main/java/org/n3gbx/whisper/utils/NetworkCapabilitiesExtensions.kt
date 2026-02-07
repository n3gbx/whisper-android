package org.n3gbx.whisper.utils

import android.net.NetworkCapabilities
import org.n3gbx.whisper.model.ConnectionType

fun NetworkCapabilities.toConnectionType(): ConnectionType =
    when {
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
        else -> ConnectionType.OTHER
    }