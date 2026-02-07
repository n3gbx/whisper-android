package org.n3gbx.whisper.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import org.n3gbx.whisper.model.ConnectionType

fun Context.connectionTypeFlow(): Flow<ConnectionType> = callbackFlow {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun emitCurrent() {
        val network = connectivityManager.activeNetwork
        val caps = network?.let { connectivityManager.getNetworkCapabilities(it) }

        val type = caps?.toConnectionType() ?: ConnectionType.NONE
        trySend(type)
    }

    emitCurrent()

    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            emitCurrent()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            caps: NetworkCapabilities
        ) {
            emitCurrent()
        }

        override fun onLost(network: Network) {
            emitCurrent()
        }
    }

    connectivityManager.registerDefaultNetworkCallback(callback)

    awaitClose {
        connectivityManager.unregisterNetworkCallback(callback)
    }
}.distinctUntilChanged()