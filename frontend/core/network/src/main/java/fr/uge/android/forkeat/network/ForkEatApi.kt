package fr.uge.android.forkeat.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import fr.uge.android.forkeat.network.api.AuthApiService
import fr.uge.android.forkeat.network.api.WalletApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import kotlin.time.Instant

class InstantAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {

    override fun serialize(src: Instant, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(src.toString())
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Instant {
        return Instant.parse(json.asString)
    }
}

object ForkEatApi {

    // 10.0.2.2 = host machine depuis l'émulateur Android
    // Pour un device physique, utiliser l'IP locale de la machine (ex: 192.168.x.x)
    private const val BASE_URL = "http://192.168.1.39:8080/" //"http://10.0.2.2:8080/"

    private var tokenManager: TokenManager? = null

    fun init(context: Context) {
        tokenManager = TokenManager(context.applicationContext)
    }

    fun isLoggedIn(): Boolean = tokenManager?.isLoggedIn() ?: false

    fun isAdmin(): Boolean = tokenManager?.isAdmin() ?: false

    fun getCurrentUsername(): String? = tokenManager?.getCurrentUsername()

    fun logout() {
        tokenManager?.clearToken()
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, InstantAdapter())
        .create()

    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                tokenManager?.getToken()?.let { token ->
                    requestBuilder.addHeader("Authorization", "$token")
                }
                chain.proceed(requestBuilder.build())
            }
            .addInterceptor(loggingInterceptor)
            .followRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    fun <T> createService(serviceClass: Class<T>): T = retrofit.create(serviceClass)

    val authService: AuthApiService by lazy { createService(AuthApiService::class.java) }
    val walletService: WalletApiService by lazy { createService(WalletApiService::class.java) }

    fun toJson(obj: Any): String = gson.toJson(obj)
}
