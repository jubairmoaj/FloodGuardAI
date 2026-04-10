package com.apppulse.floodguardai.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object DirectRouteApiProvider {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
        )
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl.ensureTrailingSlash())
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    fun googleMaps(baseUrl: String): GoogleMapsDirectApi =
        retrofit(baseUrl).create(GoogleMapsDirectApi::class.java)

    fun openWeather(baseUrl: String): OpenWeatherDirectApi =
        retrofit(baseUrl).create(OpenWeatherDirectApi::class.java)

    fun gemini(baseUrl: String): GeminiDirectApi =
        retrofit(baseUrl).create(GeminiDirectApi::class.java)
}

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
