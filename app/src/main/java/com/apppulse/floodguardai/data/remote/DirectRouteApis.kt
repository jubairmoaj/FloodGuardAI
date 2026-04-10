package com.apppulse.floodguardai.data.remote

import com.apppulse.floodguardai.data.model.GeminiGenerateRequest
import com.apppulse.floodguardai.data.model.GeminiGenerateResponse
import com.apppulse.floodguardai.data.model.GoogleDirectionsResponse
import com.apppulse.floodguardai.data.model.GoogleGeocodeResponse
import com.apppulse.floodguardai.data.model.OpenWeatherForecastResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleMapsDirectApi {
    @GET("geocode/json")
    suspend fun geocode(
        @Query("address") address: String,
        @Query("components") components: String? = null,
        @Query("region") region: String? = null,
        @Query("key") key: String
    ): GoogleGeocodeResponse

    @GET("directions/json")
    suspend fun directions(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("alternatives") alternatives: Boolean,
        @Query("mode") mode: String,
        @Query("region") region: String,
        @Query("key") key: String
    ): GoogleDirectionsResponse
}

interface OpenWeatherDirectApi {
    @GET("forecast")
    suspend fun forecast(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("units") units: String,
        @Query("appid") apiKey: String
    ): OpenWeatherForecastResponse
}

interface GeminiDirectApi {
    @POST("models/{model}:generateContent")
    suspend fun generateRouteDecision(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body payload: GeminiGenerateRequest
    ): GeminiGenerateResponse
}
