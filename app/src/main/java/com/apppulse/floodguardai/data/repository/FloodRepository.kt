package com.apppulse.floodguardai.data.repository

import android.net.Uri
import com.apppulse.floodguardai.data.model.AlertHistoryResponse
import com.apppulse.floodguardai.data.model.AlertsResponse
import com.apppulse.floodguardai.data.model.AuthResponse
import com.apppulse.floodguardai.data.model.ChatAskResponse
import com.apppulse.floodguardai.data.model.LocationInput
import com.apppulse.floodguardai.data.model.MapLayersResponse
import com.apppulse.floodguardai.data.model.PredictionResponse
import com.apppulse.floodguardai.data.model.ReportResponse
import com.apppulse.floodguardai.data.model.RouteAnalyzeResponse
import com.apppulse.floodguardai.data.model.UserProfile
import com.apppulse.floodguardai.ui.state.AppLanguage

interface FloodRepository {
    suspend fun register(name: String, email: String, password: String): AuthResponse
    suspend fun login(email: String, password: String): AuthResponse
    suspend fun me(): UserProfile?
    fun logout()
    fun isLoggedIn(): Boolean

    suspend fun getLocationPrediction(location: LocationInput, language: AppLanguage): PredictionResponse
    suspend fun getMapLayers(): MapLayersResponse
    suspend fun askAi(question: String, location: LocationInput?, language: AppLanguage): ChatAskResponse
    suspend fun analyzeRoute(
        from: LocationInput,
        to: LocationInput,
        time: String,
        language: AppLanguage
    ): RouteAnalyzeResponse

    suspend fun uploadFloodReport(
        imageUri: Uri,
        location: LocationInput,
        note: String
    ): ReportResponse

    suspend fun getAlerts(): AlertsResponse
    suspend fun createAlert(location: LocationInput, threshold: Int): AlertsResponse
    suspend fun deleteAlert(id: Long): AlertsResponse
    suspend fun getAlertHistory(): AlertHistoryResponse
    suspend fun registerPushToken(token: String)
}

data class RepositoryConfig(
    val baseUrl: String
)
