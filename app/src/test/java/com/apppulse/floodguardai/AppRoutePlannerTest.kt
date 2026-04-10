package com.apppulse.floodguardai

import com.apppulse.floodguardai.data.model.GeminiGenerateRequest
import com.apppulse.floodguardai.data.model.GeminiGenerateResponse
import com.apppulse.floodguardai.data.model.GoogleDirectionsLeg
import com.apppulse.floodguardai.data.model.GoogleDirectionsResponse
import com.apppulse.floodguardai.data.model.GoogleDirectionsRoute
import com.apppulse.floodguardai.data.model.GoogleGeocodeGeometry
import com.apppulse.floodguardai.data.model.GoogleGeocodeLocation
import com.apppulse.floodguardai.data.model.GoogleGeocodeResponse
import com.apppulse.floodguardai.data.model.GoogleGeocodeResult
import com.apppulse.floodguardai.data.model.GooglePolyline
import com.apppulse.floodguardai.data.model.GoogleValueText
import com.apppulse.floodguardai.data.model.LocationInput
import com.apppulse.floodguardai.data.model.OpenWeatherHourly
import com.apppulse.floodguardai.data.model.OpenWeatherForecastResponse
import com.apppulse.floodguardai.data.model.OpenWeatherRain
import com.apppulse.floodguardai.data.repository.AppRoutePlanner
import com.apppulse.floodguardai.data.repository.RepositoryConfig
import com.apppulse.floodguardai.data.remote.GeminiDirectApi
import com.apppulse.floodguardai.data.remote.GoogleMapsDirectApi
import com.apppulse.floodguardai.data.remote.OpenWeatherDirectApi
import com.apppulse.floodguardai.ui.state.AppLanguage
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class AppRoutePlannerTest {
    @Test
    fun analyze_route_changes_with_departure_time() = runTest {
        val planner = AppRoutePlanner(
            config = testConfig(),
            googleMapsApi = FakeGoogleMapsApi(),
            openWeatherApi = FakeOpenWeatherApi(),
            geminiApi = FakeGeminiApi()
        )

        val from = LocationInput(0.0, 0.0, "Dhanmondi")
        val to = LocationInput(0.0, 0.0, "Bashundhara")

        val afternoon = planner.analyzeRoute(from, to, "16:00", AppLanguage.English)
        val evening = planner.analyzeRoute(from, to, "18:00", AppLanguage.English)

        assertThat(afternoon.riskPercent).isGreaterThan(evening.riskPercent)
        assertThat(afternoon.chosenRoute).isNotEqualTo(evening.chosenRoute)
        assertThat(afternoon.riskySections).isNotEmpty()
    }

    @Test
    fun analyze_route_surfaces_provider_errors() = runTest {
        val planner = AppRoutePlanner(
            config = testConfig(),
            googleMapsApi = object : FakeGoogleMapsApi() {
                override suspend fun geocode(address: String, key: String): GoogleGeocodeResponse {
                    return GoogleGeocodeResponse(status = "ZERO_RESULTS", results = emptyList())
                }
            },
            openWeatherApi = FakeOpenWeatherApi(),
            geminiApi = FakeGeminiApi()
        )

        val error = runCatching {
            planner.analyzeRoute(
                from = LocationInput(0.0, 0.0, "Unknown place"),
                to = LocationInput(0.0, 0.0, "Elsewhere"),
                time = "16:00",
                language = AppLanguage.English
            )
        }.exceptionOrNull()

        assertThat(error).isNotNull()
        assertThat(error?.message).contains("Could not resolve")
    }
}

private open class FakeGoogleMapsApi : GoogleMapsDirectApi {
    private val primaryRoute = listOf(
        23.8000 to 90.3900,
        23.8100 to 90.4000,
        23.8200 to 90.4100
    )
    private val secondaryRoute = listOf(
        23.7200 to 90.3600,
        23.7300 to 90.3700,
        23.7400 to 90.3800
    )

    override suspend fun geocode(address: String, key: String): GoogleGeocodeResponse {
        val point = when (address) {
            "Dhanmondi" -> 23.7465 to 90.3760
            "Bashundhara" -> 23.8151 to 90.4256
            else -> return GoogleGeocodeResponse(status = "ZERO_RESULTS", results = emptyList())
        }
        return GoogleGeocodeResponse(
            status = "OK",
            results = listOf(
                GoogleGeocodeResult(
                    formattedAddress = address,
                    geometry = GoogleGeocodeGeometry(
                        location = GoogleGeocodeLocation(lat = point.first, lng = point.second)
                    )
                )
            )
        )
    }

    override suspend fun directions(
        origin: String,
        destination: String,
        alternatives: Boolean,
        mode: String,
        region: String,
        key: String
    ): GoogleDirectionsResponse {
        return GoogleDirectionsResponse(
            status = "OK",
            routes = listOf(
                route("Primary Corridor", primaryRoute, durationSeconds = 3600, distanceMeters = 16000),
                route("Lakeside Bypass", secondaryRoute, durationSeconds = 3900, distanceMeters = 17000)
            )
        )
    }

    private fun route(
        name: String,
        points: List<Pair<Double, Double>>,
        durationSeconds: Int,
        distanceMeters: Int
    ): GoogleDirectionsRoute {
        return GoogleDirectionsRoute(
            summary = name,
            legs = listOf(
                GoogleDirectionsLeg(
                    distance = GoogleValueText(text = "${distanceMeters / 1000} km", value = distanceMeters),
                    duration = GoogleValueText(text = "${durationSeconds / 60} mins", value = durationSeconds)
                )
            ),
            overviewPolyline = GooglePolyline(points = encodePolyline(points))
        )
    }
}

private class FakeOpenWeatherApi : OpenWeatherDirectApi {
    override suspend fun forecast(
        lat: Double,
        lon: Double,
        units: String,
        apiKey: String
    ): OpenWeatherForecastResponse {
        val hourly = buildHourlyForecast(
            if (lat >= 23.78) {
                mapOf(
                    16 to 7.2,
                    17 to 4.8,
                    18 to 1.1,
                    19 to 0.4,
                    20 to 0.2
                )
            } else {
                mapOf(
                    16 to 1.6,
                    17 to 2.1,
                    18 to 6.4,
                    19 to 5.2,
                    20 to 3.0
                )
            }
        )
        return OpenWeatherForecastResponse(list = hourly)
    }
}

private class FakeGeminiApi : GeminiDirectApi {
    override suspend fun generateRouteDecision(
        model: String,
        apiKey: String,
        payload: GeminiGenerateRequest
    ): GeminiGenerateResponse = GeminiGenerateResponse(emptyList())
}

private fun buildHourlyForecast(rainByHour: Map<Int, Double>): List<OpenWeatherHourly> {
    val zone = ZoneId.systemDefault()
    val date = LocalDate.now()
    return rainByHour.map { (hour, rain) ->
        val timestamp = LocalDateTime.of(date, LocalTime.of(hour, 0))
            .atZone(zone)
            .toEpochSecond()
        OpenWeatherHourly(
            dt = timestamp,
            rain = OpenWeatherRain(oneHour = rain)
        )
    }
}

private fun encodePolyline(points: List<Pair<Double, Double>>): String {
    val builder = StringBuilder()
    var previousLat = 0
    var previousLng = 0

    points.forEach { (lat, lng) ->
        val encodedLat = (lat * 1e5).toInt()
        val encodedLng = (lng * 1e5).toInt()
        builder.append(encodeValue(encodedLat - previousLat))
        builder.append(encodeValue(encodedLng - previousLng))
        previousLat = encodedLat
        previousLng = encodedLng
    }
    return builder.toString()
}

private fun encodeValue(value: Int): String {
    var current = if (value < 0) (value shl 1).inv() else value shl 1
    val builder = StringBuilder()
    while (current >= 0x20) {
        builder.append(((0x20 or (current and 0x1f)) + 63).toChar())
        current = current shr 5
    }
    builder.append((current + 63).toChar())
    return builder.toString()
}

private fun testConfig() = RepositoryConfig(
    baseUrl = "https://example.com/api/",
    googleMapsRestBaseUrl = "https://maps.googleapis.com/maps/api/",
    googleMapsApiKey = "maps-key",
    openWeatherBaseUrl = "https://api.openweathermap.org/data/3.0/",
    openWeatherApiKey = "weather-key",
    geminiBaseUrl = "https://generativelanguage.googleapis.com/v1beta/",
    geminiApiKey = "gemini-key",
    geminiModel = "gemini-2.0-flash"
)
