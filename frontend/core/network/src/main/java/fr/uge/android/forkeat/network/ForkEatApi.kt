package fr.uge.android.forkeat.network

import fr.uge.android.forkeat.network.api.AuthApiService
import fr.uge.android.forkeat.recipes.data.api.RecipeApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ForkEatApi {

    // 10.0.2.2 = host machine depuis l'émulateur Android
    // Pour un device physique, utiliser l'IP locale de la machine (ex: 192.168.x.x)
    private const val BASE_URL = "http://10.0.2.2:8080/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authService: AuthApiService = retrofit.create(AuthApiService::class.java)
    val recipeService: RecipeApiService = retrofit.create(RecipeApiService::class.java)
}
