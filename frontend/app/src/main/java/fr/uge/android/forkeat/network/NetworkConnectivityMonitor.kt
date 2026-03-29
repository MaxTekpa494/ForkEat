package fr.uge.android.forkeat.network

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import android.util.Log
import fr.uge.android.forkeat.service.PromotionSseService

/**
 * Démarre le service promotions quand Internet est disponible, l'arrête quand la connexion est perdue.
 */
object NetworkConnectivityMonitor {
    private var isRegistered = false
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var appContext: Context

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val serviceIntent = Intent(appContext, PromotionSseService::class.java)
            appContext.startService(serviceIntent)
        }

        override fun onLost(network: Network) {
            val serviceIntent = Intent(appContext, PromotionSseService::class.java)
            appContext.stopService(serviceIntent)
        }
    }

    fun register(context: Context) {
        if (isRegistered) return
        appContext = context.applicationContext
        connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().build()
        connectivityManager.registerNetworkCallback(request, networkCallback)
        isRegistered = true
    }

    fun unregister() {
        if (!isRegistered) return
        connectivityManager.unregisterNetworkCallback(networkCallback)
        isRegistered = false
    }
}

