package fr.uge.android.forkeat.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import fr.uge.android.forkeat.ForkEatApplication
import fr.uge.android.forkeat.MainActivity
import fr.uge.android.forkeat.PromotionEventBus
import fr.uge.android.forkeat.network.ForkEatApi
import fr.uge.android.forkeat.network.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class PromotionSseService : Service() {

    companion object {
        const val NOTIFICATION_ID_SERVICE = 1001
        const val NOTIFICATION_ID_PROMOTION = 1002
    }

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // Client dédié SSE : readTimeout=0 pour garder la connexion ouverte indéfiniment
    private val sseClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID_SERVICE, buildServiceNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { connectWithRetry() }
        return START_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        sseClient.dispatcher.executorService.shutdown()
        super.onDestroy()
    }

    private suspend fun connectWithRetry() {
        var backoffMs = 5_000L
        while (scope.isActive) {
            try {
                connect()
                backoffMs = 5_000L // reset backoff après une connexion réussie
            } catch (e: IOException) {
                delay(backoffMs)
                backoffMs = minOf(backoffMs * 2, 60_000L) // backoff exponentiel, max 1 min
            }
        }
    }

    private suspend fun connect() = withContext(Dispatchers.IO) {
        val token = TokenManager(applicationContext).getToken() ?: run {
            stopSelf()
            return@withContext
        }

        val request = Request.Builder()
            .url("${ForkEatApi.getBaseUrl()}api/promotions/stream")
            .header("Authorization", token)
            .header("Accept", "text/event-stream")
            .header("Cache-Control", "no-cache")
            .build()

        val call = sseClient.newCall(request)
        try {
            val response = call.execute()
            if (response.code == 401) {
                stopSelf()
                return@withContext
            }
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

            val source = response.body?.source() ?: throw IOException("Corps de réponse vide")
            var pendingEventName: String? = null
            var pendingData: String? = null

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.startsWith("event:") -> pendingEventName = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> pendingData = line.removePrefix("data:").trim()
                    line.isEmpty() -> {
                        pendingData?.let { handleEvent(pendingEventName, it) }
                        pendingEventName = null
                        pendingData = null
                    }
                }
            }
        } finally {
            call.cancel()
        }
    }

    private fun handleEvent(eventName: String?, data: String) {
        try {
            val json = JSONObject(data)
            val type = json.optString("type").ifEmpty { eventName ?: return }
            val promotion = json.optJSONObject("promotion") ?: return
            val name = promotion.optString("name", "Promotion")
            val priceCents = promotion.optLong("priceCents", 0)
            val bonusEveryN = if (promotion.isNull("bonusEveryN")) null else promotion.optInt("bonusEveryN")

            val (title, body) = when (type.uppercase()) {
                "ACTIVATED" -> buildActivatedMessage(name, priceCents, bonusEveryN)
                "EXPIRED" -> "Fin de promotion" to "La promotion \"$name\" est terminée"
                else -> return
            }
            showPromotionNotification(title, body)
            PromotionEventBus.emit(PromotionEventBus.Event(title, body))
        } catch (_: Exception) {
            // événement malformé, ignoré silencieusement
        }
    }

    private fun buildActivatedMessage(name: String, priceCents: Long, bonusEveryN: Int?): Pair<String, String> {
        val price = String.format("%.2f€", priceCents / 100.0)
        val body = buildString {
            append("\"$name\" — Super-like à $price")
            if (bonusEveryN != null) append(", 1 gratuit tous les $bonusEveryN")
        }
        return "Promotion en cours !" to body
    }

    private fun showPromotionNotification(title: String, body: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ForkEatApplication.CHANNEL_PROMOTIONS)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID_PROMOTION, notification)
    }

    private fun buildServiceNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, ForkEatApplication.CHANNEL_SSE_SERVICE)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("ForkEat")
            .setContentText("Notifications activées")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }
}
