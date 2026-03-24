package fr.uge.android.forkeat

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class ForkEatApplication : Application() {

    companion object {
        const val CHANNEL_PROMOTIONS = "channel_promotions"
        const val CHANNEL_SSE_SERVICE = "channel_sse_service"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val promotionChannel = NotificationChannel(
                CHANNEL_PROMOTIONS,
                "Promotions ForkEat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertes de démarrage et fin de promotions super-like"
            }

            val serviceChannel = NotificationChannel(
                CHANNEL_SSE_SERVICE,
                "Service ForkEat",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Connexion aux notifications en temps réel"
            }

            manager.createNotificationChannels(listOf(promotionChannel, serviceChannel))
        }
    }
}
