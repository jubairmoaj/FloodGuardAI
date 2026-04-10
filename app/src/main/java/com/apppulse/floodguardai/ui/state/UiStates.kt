package com.apppulse.floodguardai.ui.state

import com.apppulse.floodguardai.data.model.AlertEvent
import com.apppulse.floodguardai.data.model.AlertItem
import com.apppulse.floodguardai.data.model.ChatAskResponse
import com.apppulse.floodguardai.data.model.MapLayersResponse
import com.apppulse.floodguardai.data.model.PredictionResponse
import com.apppulse.floodguardai.data.model.ReportAnalysis
import com.apppulse.floodguardai.data.model.RouteAnalyzeResponse

enum class AppLanguage(val code: String) {
    Bangla("bn"),
    English("en")
}

data class DashboardUiState(
    val isLoading: Boolean = false,
    val locationLabel: String = "Dhaka",
    val rainMmHour: Double = 0.0,
    val prediction: PredictionResponse? = null,
    val error: String? = null
)

data class MapUiState(
    val isLoading: Boolean = false,
    val layers: MapLayersResponse? = null,
    val error: String? = null
)

data class ChatMessage(
    val fromUser: Boolean,
    val text: String
)

data class ChatUiState(
    val isLoading: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val latestStructured: ChatAskResponse? = null,
    val error: String? = null
)

data class RouteUiState(
    val isLoading: Boolean = false,
    val fromText: String = "",
    val toText: String = "",
    val timeText: String = "16:00",
    val response: RouteAnalyzeResponse? = null,
    val error: String? = null
)

data class ReportUiState(
    val isLoading: Boolean = false,
    val note: String = "",
    val imageUri: String? = null,
    val lastAnalysis: ReportAnalysis? = null,
    val error: String? = null
)

data class AlertsUiState(
    val isLoading: Boolean = false,
    val threshold: Int = 60,
    val alerts: List<AlertItem> = emptyList(),
    val events: List<AlertEvent> = emptyList(),
    val error: String? = null
)

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.English,
    val notificationsEnabled: Boolean = true,
    val savedLocations: List<String> = listOf("Dhaka", "Chattogram"),
    val isLoggedIn: Boolean = false,
    val userName: String? = null,
    val userEmail: String? = null,
    val authError: String? = null,
    val isAuthLoading: Boolean = false
)
