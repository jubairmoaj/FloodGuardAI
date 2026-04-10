package com.apppulse.floodguardai.data.remote

import com.apppulse.floodguardai.data.model.AlertHistoryResponse
import com.apppulse.floodguardai.data.model.AlertRequest
import com.apppulse.floodguardai.data.model.AlertsResponse
import com.apppulse.floodguardai.data.model.AuthRequest
import com.apppulse.floodguardai.data.model.AuthResponse
import com.apppulse.floodguardai.data.model.ChatAskRequest
import com.apppulse.floodguardai.data.model.ChatAskResponse
import com.apppulse.floodguardai.data.model.DeviceTokenRequest
import com.apppulse.floodguardai.data.model.MapLayersResponse
import com.apppulse.floodguardai.data.model.PredictionRequest
import com.apppulse.floodguardai.data.model.PredictionResponse
import com.apppulse.floodguardai.data.model.RefreshRequest
import com.apppulse.floodguardai.data.model.ReportResponse
import com.apppulse.floodguardai.data.model.RouteAnalyzeRequest
import com.apppulse.floodguardai.data.model.RouteAnalyzeResponse
import com.apppulse.floodguardai.data.model.UserProfile
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface FloodGuardApi {
    @POST("auth/register")
    suspend fun register(@Body payload: AuthRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body payload: AuthRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body payload: RefreshRequest): AuthResponse

    @GET("me")
    suspend fun me(): UserProfile

    @POST("predictions/location")
    suspend fun predictLocation(@Body payload: PredictionRequest): PredictionResponse

    @POST("routes/analyze")
    suspend fun analyzeRoute(@Body payload: RouteAnalyzeRequest): RouteAnalyzeResponse

    @GET("map/layers")
    suspend fun mapLayers(): MapLayersResponse

    @Multipart
    @POST("reports")
    suspend fun uploadReport(
        @Part image: MultipartBody.Part,
        @Part("lat") lat: RequestBody,
        @Part("lng") lng: RequestBody,
        @Part("note") note: RequestBody
    ): ReportResponse

    @POST("chat/ask")
    suspend fun askChat(@Body payload: ChatAskRequest): ChatAskResponse

    @POST("alerts")
    suspend fun createAlert(@Body payload: AlertRequest): AlertsResponse

    @GET("alerts")
    suspend fun getAlerts(): AlertsResponse

    @DELETE("alerts/{id}")
    suspend fun deleteAlert(@Path("id") id: Long): AlertsResponse

    @GET("alerts/history")
    suspend fun alertHistory(): AlertHistoryResponse

    @POST("device-tokens")
    suspend fun registerDeviceToken(@Body payload: DeviceTokenRequest): AlertsResponse
}
