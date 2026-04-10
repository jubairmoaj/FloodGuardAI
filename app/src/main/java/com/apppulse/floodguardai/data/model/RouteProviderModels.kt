package com.apppulse.floodguardai.data.model

import com.squareup.moshi.Json

data class GoogleGeocodeResponse(
    val status: String,
    val results: List<GoogleGeocodeResult> = emptyList(),
    @Json(name = "error_message")
    val errorMessage: String? = null
)

data class GoogleGeocodeResult(
    @Json(name = "formatted_address")
    val formattedAddress: String,
    val geometry: GoogleGeocodeGeometry
)

data class GoogleGeocodeGeometry(
    val location: GoogleGeocodeLocation
)

data class GoogleGeocodeLocation(
    val lat: Double,
    val lng: Double
)

data class GoogleDirectionsResponse(
    val status: String,
    val routes: List<GoogleDirectionsRoute> = emptyList(),
    @Json(name = "error_message")
    val errorMessage: String? = null
)

data class GoogleDirectionsRoute(
    val summary: String = "",
    val legs: List<GoogleDirectionsLeg> = emptyList(),
    @Json(name = "overview_polyline")
    val overviewPolyline: GooglePolyline
)

data class GoogleDirectionsLeg(
    val distance: GoogleValueText,
    val duration: GoogleValueText
)

data class GoogleValueText(
    val text: String,
    val value: Int
)

data class GooglePolyline(
    val points: String
)

data class OpenWeatherForecastResponse(
    val list: List<OpenWeatherHourly> = emptyList()
)

data class OpenWeatherHourly(
    val dt: Long,
    val rain: OpenWeatherRain? = null
)

data class OpenWeatherRain(
    @Json(name = "1h")
    val oneHour: Double? = null,
    @Json(name = "3h")
    val threeHour: Double? = null
)

data class GeminiGenerateRequest(
    val contents: List<GeminiContent>,
    @Json(name = "generationConfig")
    val generationConfig: GeminiGenerationConfig
)

data class GeminiContent(
    val parts: List<GeminiTextPart>
)

data class GeminiTextPart(
    val text: String
)

data class GeminiGenerationConfig(
    @Json(name = "responseMimeType")
    val responseMimeType: String = "application/json"
)

data class GeminiGenerateResponse(
    val candidates: List<GeminiCandidate> = emptyList()
)

data class GeminiCandidate(
    val content: GeminiContent? = null
)

data class GeminiRouteDecision(
    val decision: String = "",
    @Json(name = "recommended_action")
    val recommendedAction: String = "",
    val explanation: String = "",
    @Json(name = "chosen_route")
    val chosenRoute: String = ""
)
