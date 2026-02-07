package org.n3gbx.whisper.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class IsConnectedToInternet @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    operator fun invoke(): Flow<Boolean> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        fun emitCurrent() {
            val network = connectivityManager.activeNetwork
            val caps = network?.let { connectivityManager.getNetworkCapabilities(it) }

            val isWifiValidated =
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true &&
                    caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            trySend(isWifiValidated)
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
}