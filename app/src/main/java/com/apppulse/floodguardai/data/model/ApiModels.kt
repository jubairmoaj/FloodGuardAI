package com.apppulse.floodguardai.data.model

import com.squareup.moshi.Json

data class Coordinate(
    val lat: Double,
    val lng: Double
)

data class LocationInput(
    val lat: Double,
    val lng: Double,
    val name: String? = null
)

data class AuthRequest(
    val name: String? = null,
    val email: String,
    val password: String
)

data class RefreshRequest(
    @Json(name = "refresh_token")
    val refreshToken: String
)

data class AuthResponse(
    @Json(name = "access_token")
    val accessToken: String,
    @Json(name = "refresh_token")
    val refreshToken: String,
    val user: UserProfile
)

data class UserProfile(
    val id: Long,
    val name: String,
    val email: String,
    val language: String = "en"
)

data class PredictionRequest(
    val location: LocationInput,
    val language: String = "en"
)

data class PredictionResponse(
    @Json(name = "flood_probability")
    val floodProbability: Int,
    @Json(name = "peak_risk_time")
    val peakRiskTime: String,
    @Json(name = "risk_level")
    val riskLevel: String,
    val explanation: String,
    val confidence: Int
)

data class RouteAnalyzeRequest(
    val from: LocationInput,
    val to: LocationInput,
    val time: String,
    val language: String = "en"
)

data class RouteAlternative(
    val route: String,
    @Json(name = "risk_percent")
    val riskPercent: Int,
    @Json(name = "recommended_time")
    val recommendedTime: String,
    val reason: String = ""
)

data class RouteResolvedLocation(
    val lat: Double,
    val lng: Double,
    val name: String
)

data class RouteRiskSection(
    val label: String,
    val lat: Double,
    val lng: Double,
    @Json(name = "risk_percent")
    val riskPercent: Int,
    @Json(name = "risk_level")
    val riskLevel: String,
    @Json(name = "rain_mm")
    val rainMm: Double,
    @Json(name = "forecast_time")
    val forecastTime: String = "",
    @Json(name = "history_severity_avg")
    val historySeverityAvg: Double = 0.0,
    @Json(name = "report_depth_avg_cm")
    val reportDepthAvgCm: Double = 0.0
)

data class RouteAnalyzeResponse(
    val decision: String,
    @Json(name = "risk_percent")
    val riskPercent: Int,
    @Json(name = "recommended_time")
    val recommendedTime: String,
    @Json(name = "recommended_action")
    val recommendedAction: String,
    val explanation: String = "",
    @Json(name = "chosen_route")
    val chosenRoute: String = "",
    @Json(name = "resolved_origin")
    val resolvedOrigin: RouteResolvedLocation? = null,
    @Json(name = "resolved_destination")
    val resolvedDestination: RouteResolvedLocation? = null,
    @Json(name = "risky_sections")
    val riskySections: List<RouteRiskSection> = emptyList(),
    val alternatives: List<RouteAlternative>
)

data class MapPolygon(
    val id: String,
    val points: List<Coordinate>,
    @Json(name = "risk_level")
    val riskLevel: String
)

data class MapReportPin(
    val id: Long,
    val lat: Double,
    val lng: Double,
    @Json(name = "image_url")
    val imageUrl: String? = null,
    @Json(name = "water_level")
    val waterLevel: String? = null,
    val note: String? = null,
    @Json(name = "created_at")
    val createdAt: String? = null
)

data class MapLayersResponse(
    @Json(name = "flood_zones")
    val floodZones: List<MapPolygon>,
    @Json(name = "safe_zones")
    val safeZones: List<MapPolygon>,
    val reports: List<MapReportPin>
)

data class ReportAnalysis(
    @Json(name = "estimated_depth_cm")
    val estimatedDepthCm: Int,
    @Json(name = "time_to_clear_min")
    val timeToClearMin: Int,
    @Json(name = "risk_level")
    val riskLevel: String,
    val confidence: Int
)

data class ReportResponse(
    @Json(name = "report_id")
    val reportId: Long,
    @Json(name = "image_url")
    val imageUrl: String,
    val analysis: ReportAnalysis
)

data class ChatAskRequest(
    val question: String,
    val context: ChatContext,
    val language: String = "en"
)

data class ChatContext(
    val location: LocationInput? = null,
    @Json(name = "rain_mm_hour")
    val rainMmHour: Double? = null,
    @Json(name = "flood_probability")
    val floodProbability: Int? = null
)

data class ChatAskResponse(
    val answer: String,
    @Json(name = "risk_level")
    val riskLevel: String? = null,
    @Json(name = "recommended_action")
    val recommendedAction: String? = null
)

data class AlertRequest(
    val location: LocationInput,
    val threshold: Int
)

data class AlertItem(
    val id: Long,
    val location: String,
    val threshold: Int,
    val enabled: Boolean = true
)

data class AlertEvent(
    val id: Long,
    @Json(name = "alert_id")
    val alertId: Long,
    val message: String,
    @Json(name = "triggered_at")
    val triggeredAt: String
)

data class AlertsResponse(
    val alerts: List<AlertItem>
)

data class AlertHistoryResponse(
    val events: List<AlertEvent>
)

data class DeviceTokenRequest(
    @Json(name = "device_token")
    val deviceToken: String,
    val platform: String = "android"
)
