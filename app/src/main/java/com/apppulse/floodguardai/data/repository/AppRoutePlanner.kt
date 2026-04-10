package com.apppulse.floodguardai.data.repository

import com.apppulse.floodguardai.data.model.Coordinate
import com.apppulse.floodguardai.data.model.GeminiContent
import com.apppulse.floodguardai.data.model.GeminiGenerateRequest
import com.apppulse.floodguardai.data.model.GeminiGenerationConfig
import com.apppulse.floodguardai.data.model.GeminiRouteDecision
import com.apppulse.floodguardai.data.model.GeminiTextPart
import com.apppulse.floodguardai.data.model.GoogleDirectionsRoute
import com.apppulse.floodguardai.data.model.LocationInput
import com.apppulse.floodguardai.data.model.OpenWeatherHourly
import com.apppulse.floodguardai.data.model.RouteAlternative
import com.apppulse.floodguardai.data.model.RouteAnalyzeResponse
import com.apppulse.floodguardai.data.model.RouteResolvedLocation
import com.apppulse.floodguardai.data.model.RouteRiskSection
import com.apppulse.floodguardai.data.remote.DirectRouteApiProvider
import com.apppulse.floodguardai.data.remote.GeminiDirectApi
import com.apppulse.floodguardai.data.remote.GoogleMapsDirectApi
import com.apppulse.floodguardai.data.remote.OpenWeatherDirectApi
import com.apppulse.floodguardai.ui.state.AppLanguage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class AppRoutePlanner(
    private val config: RepositoryConfig,
    private val googleMapsApi: GoogleMapsDirectApi = DirectRouteApiProvider.googleMaps(config.googleMapsRestBaseUrl),
    private val openWeatherApi: OpenWeatherDirectApi = DirectRouteApiProvider.openWeather(config.openWeatherBaseUrl),
    private val geminiApi: GeminiDirectApi = DirectRouteApiProvider.gemini(config.geminiBaseUrl)
) {
    private val jsonAdapter = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()
        .adapter(GeminiRouteDecision::class.java)

    suspend fun analyzeRoute(
        from: LocationInput,
        to: LocationInput,
        time: String,
        language: AppLanguage
    ): RouteAnalyzeResponse {
        require(config.googleMapsApiKey.isNotBlank()) { "Google Maps API key is missing." }
        require(config.openWeatherApiKey.isNotBlank()) { "OpenWeather API key is missing." }

        val departure = resolveDepartureTime(time)
        val origin = resolveLocation(from)
        val destination = resolveLocation(to)
        val routeResponse = googleMapsApi.directions(
            origin = "${origin.lat},${origin.lng}",
            destination = "${destination.lat},${destination.lng}",
            alternatives = true,
            mode = "driving",
            region = "bd",
            key = config.googleMapsApiKey
        )
        if (routeResponse.status != "OK" || routeResponse.routes.isEmpty()) {
            error(routeResponse.errorMessage ?: "Unable to fetch route alternatives.")
        }

        val weatherCache = mutableMapOf<String, List<OpenWeatherHourly>>()
        val candidates = routeResponse.routes.take(3).mapIndexed { index, route ->
            analyzeCandidate(
                index = index,
                route = route,
                departure = departure,
                weatherCache = weatherCache
            )
        }.sortedBy { it.riskPercent }

        val best = candidates.first()
        val aiDecision = requestGeminiDecision(
            language = language,
            origin = origin,
            destination = destination,
            best = best,
            candidates = candidates
        )

        val decision = aiDecision?.decision?.takeIf { it.isNotBlank() } ?: defaultDecision(best.riskPercent)
        val recommendedAction = aiDecision?.recommendedAction?.takeIf { it.isNotBlank() }
            ?: defaultRecommendedAction(best.riskPercent, best.recommendedTime)
        val explanation = aiDecision?.explanation?.takeIf { it.isNotBlank() }
            ?: buildFallbackExplanation(best)
        val chosenRoute = aiDecision?.chosenRoute?.takeIf { it.isNotBlank() } ?: best.name

        return RouteAnalyzeResponse(
            decision = decision,
            riskPercent = best.riskPercent,
            recommendedTime = best.recommendedTime,
            recommendedAction = recommendedAction,
            explanation = explanation,
            chosenRoute = chosenRoute,
            resolvedOrigin = origin,
            resolvedDestination = destination,
            riskySections = best.riskySections,
            alternatives = candidates.drop(1).map { candidate ->
                RouteAlternative(
                    route = candidate.name,
                    riskPercent = candidate.riskPercent,
                    recommendedTime = candidate.recommendedTime,
                    reason = buildAlternativeReason(candidate, best)
                )
            }
        )
    }

    private suspend fun resolveLocation(input: LocationInput): RouteResolvedLocation {
        if ((input.lat != 0.0 || input.lng != 0.0) && !input.name.isNullOrBlank()) {
            return RouteResolvedLocation(lat = input.lat, lng = input.lng, name = input.name)
        }
        val rawQuery = input.name?.takeIf { it.isNotBlank() }
            ?: error("A location name is required for route planning.")
        val query = normalizeLocationQuery(rawQuery)
        val response = googleMapsApi.geocode(
            address = query,
            components = "country:BD",
            region = "bd",
            key = config.googleMapsApiKey
        )
        if (response.status != "OK" || response.results.isEmpty()) {
            error(response.errorMessage ?: "Could not resolve $rawQuery.")
        }
        val first = response.results.first()
        return RouteResolvedLocation(
            lat = first.geometry.location.lat,
            lng = first.geometry.location.lng,
            name = first.formattedAddress
        )
    }

    private suspend fun analyzeCandidate(
        index: Int,
        route: GoogleDirectionsRoute,
        departure: LocalDateTime,
        weatherCache: MutableMap<String, List<OpenWeatherHourly>>
    ): RouteCandidate {
        val leg = route.legs.firstOrNull() ?: error("Route details are missing.")
        val routeName = route.summary.ifBlank { "Route ${index + 1}" }
        val coordinates = decodePolyline(route.overviewPolyline.points)
            .ifEmpty { error("Route geometry is missing.") }
        val samples = sampleRoutePoints(coordinates, maxPoints = 6)
        val pointRisks = samples.map { point ->
            val forecast = forecastForPoint(point, departure, weatherCache)
            val pointRisk = scorePointRisk(forecast.rainMm)
            PointRisk(
                label = point.label,
                lat = point.lat,
                lng = point.lng,
                rainMm = forecast.rainMm,
                forecastTime = forecast.label,
                riskPercent = pointRisk,
                riskLevel = riskLevel(pointRisk)
            )
        }
        val bestTime = recommendBestTime(samples, departure, weatherCache, leg.duration.value / 60)
        val routeRisk = scoreRouteRiskFromPoints(pointRisks, leg.duration.value / 60)
        val riskySections = pointRisks
            .filter { it.riskPercent >= 55 }
            .sortedByDescending { it.riskPercent }
            .take(3)
            .map {
                RouteRiskSection(
                    label = it.label,
                    lat = it.lat,
                    lng = it.lng,
                    riskPercent = it.riskPercent,
                    riskLevel = it.riskLevel,
                    rainMm = it.rainMm,
                    forecastTime = it.forecastTime
                )
            }

        return RouteCandidate(
            name = routeName,
            riskPercent = routeRisk,
            recommendedTime = bestTime,
            distanceText = leg.distance.text,
            durationText = leg.duration.text,
            riskySections = riskySections
        )
    }

    private suspend fun forecastForPoint(
        point: SamplePoint,
        departure: LocalDateTime,
        weatherCache: MutableMap<String, List<OpenWeatherHourly>>
    ): ForecastMatch {
        val cacheKey = "${point.lat.roundKey()},${point.lng.roundKey()}"
        val forecast = weatherCache.getOrPut(cacheKey) {
            openWeatherApi.forecast(
                lat = point.lat,
                lon = point.lng,
                units = "metric",
                apiKey = config.openWeatherApiKey
            ).list
        }
        if (forecast.isEmpty()) {
            error("No weather forecast is available for ${point.label}.")
        }
        return closestForecast(forecast, departure)
    }

    private fun closestForecast(
        hourly: List<OpenWeatherHourly>,
        target: LocalDateTime
    ): ForecastMatch {
        val zone = ZoneId.systemDefault()
        val targetEpoch = target.atZone(zone).toEpochSecond()
        val nearest = hourly.minByOrNull { kotlin.math.abs(it.dt - targetEpoch) }
            ?: error("No hourly forecast found.")
        val label = Instant.ofEpochSecond(nearest.dt)
            .atZone(zone)
            .toLocalTime()
            .format(TimeFormatter)
        return ForecastMatch(
            rainMm = nearest.rain?.oneHour ?: ((nearest.rain?.threeHour ?: 0.0) / 3.0),
            label = label
        )
    }

    private suspend fun recommendBestTime(
        points: List<SamplePoint>,
        departure: LocalDateTime,
        weatherCache: MutableMap<String, List<OpenWeatherHourly>>,
        durationMinutes: Int
    ): String {
        val best = (0..6).map { offset ->
            val candidateTime = departure.plusHours(offset.toLong())
            val sampleRisks = points.map { point ->
                val forecast = forecastForPoint(point, candidateTime, weatherCache)
                scorePointRisk(forecast.rainMm)
            }
            candidateTime to scoreRouteRiskValues(sampleRisks, durationMinutes)
        }.minByOrNull { it.second } ?: (departure to 0)

        return best.first.toLocalTime().format(TimeFormatter)
    }

    private fun scoreRouteRiskFromPoints(pointRisks: List<PointRisk>, durationMinutes: Int): Int =
        scoreRouteRiskValues(pointRisks.map { it.riskPercent }, durationMinutes)

    private fun scoreRouteRiskValues(pointRisks: List<Int>, durationMinutes: Int): Int {
        val average = pointRisks.average()
        val peak = pointRisks.maxOrNull()?.toDouble() ?: 0.0
        val durationPenalty = (durationMinutes / 6.0).coerceAtMost(14.0)
        return (average * 0.65 + peak * 0.25 + durationPenalty)
            .roundToInt()
            .coerceIn(5, 98)
    }

    private fun scorePointRisk(rainMm: Double): Int = when {
        rainMm < 0.3 -> 14
        rainMm < 1.0 -> 24
        rainMm < 2.5 -> 38
        rainMm < 5.0 -> 56
        rainMm < 8.0 -> 74
        else -> 88
    }

    private fun defaultDecision(riskPercent: Int): String = when {
        riskPercent >= 70 -> "Unsafe now"
        riskPercent >= 45 -> "Travel with caution"
        else -> "Safe to go"
    }

    private fun defaultRecommendedAction(riskPercent: Int, recommendedTime: String): String = when {
        riskPercent >= 70 -> "Delay the trip if possible and try again around $recommendedTime."
        riskPercent >= 45 -> "Use the lowest-risk route and avoid low-lying roads near heavy rain."
        else -> "Conditions look manageable, but keep monitoring rain before leaving."
    }

    private fun buildFallbackExplanation(best: RouteCandidate): String {
        val topPoint = best.riskySections.firstOrNull()
        return if (topPoint != null) {
            "This route was scored from live rain forecasts along sampled path points. ${topPoint.label} shows the highest rain exposure at ${"%.1f".format(topPoint.rainMm)} mm near ${topPoint.forecastTime}, which pushes the route risk to ${best.riskPercent}%."
        } else {
            "This route was scored from live rain forecasts along the full path. Rain exposure stays relatively low, so the route risk is ${best.riskPercent}%."
        }
    }

    private fun buildAlternativeReason(candidate: RouteCandidate, best: RouteCandidate): String = when {
        candidate.riskPercent < best.riskPercent ->
            "This option sees lower forecast rain across its sampled points."
        candidate.recommendedTime != best.recommendedTime ->
            "Rain exposure improves if you leave closer to ${candidate.recommendedTime}."
        else ->
            "This path has more rain exposure than the selected route."
    }

    private suspend fun requestGeminiDecision(
        language: AppLanguage,
        origin: RouteResolvedLocation,
        destination: RouteResolvedLocation,
        best: RouteCandidate,
        candidates: List<RouteCandidate>
    ): GeminiRouteDecision? {
        if (config.geminiApiKey.isBlank()) return null

        val prompt = buildString {
            appendLine("You are a flood-aware route safety assistant.")
            appendLine("Reply in ${if (language == AppLanguage.Bangla) "Bangla" else "English"}.")
            appendLine("Return strict JSON with keys: decision, recommended_action, explanation, chosen_route.")
            appendLine("Origin: ${origin.name}")
            appendLine("Destination: ${destination.name}")
            appendLine("Best route candidate: ${best.name}")
            appendLine("Best route risk: ${best.riskPercent}%")
            appendLine("Best route recommended time: ${best.recommendedTime}")
            appendLine("Candidates:")
            candidates.forEach { candidate ->
                appendLine(
                    "- ${candidate.name}: risk ${candidate.riskPercent}%, distance ${candidate.distanceText}, duration ${candidate.durationText}, recommended time ${candidate.recommendedTime}"
                )
                if (candidate.riskySections.isNotEmpty()) {
                    candidate.riskySections.forEach { section ->
                        appendLine(
                            "  section ${section.label}: ${section.riskPercent}% risk, ${"%.1f".format(section.rainMm)} mm rain near ${section.forecastTime}"
                        )
                    }
                }
            }
            appendLine("Keep the recommendation concise and practical.")
        }

        return runCatching {
            val response = geminiApi.generateRouteDecision(
                model = config.geminiModel,
                apiKey = config.geminiApiKey,
                payload = GeminiGenerateRequest(
                    contents = listOf(
                        GeminiContent(parts = listOf(GeminiTextPart(text = prompt)))
                    ),
                    generationConfig = GeminiGenerationConfig()
                )
            )
            val rawText = response.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: return null
            jsonAdapter.fromJson(rawText)
        }.getOrNull()
    }

    private fun sampleRoutePoints(points: List<Coordinate>, maxPoints: Int): List<SamplePoint> {
        if (points.isEmpty()) return emptyList()
        val sampleIndices = if (points.size <= maxPoints) {
            points.indices.toList()
        } else {
            (0 until maxPoints).map { index ->
                ((points.lastIndex * index.toDouble()) / (maxPoints - 1)).roundToInt()
            }.distinct()
        }
        return sampleIndices.mapIndexed { index, pointIndex ->
            val point = points[pointIndex]
            SamplePoint(
                label = "Point ${index + 1}",
                lat = point.lat,
                lng = point.lng
            )
        }
    }

    private fun decodePolyline(encoded: String): List<Coordinate> {
        val poly = mutableListOf<Coordinate>()
        var index = 0
        var lat = 0
        var lng = 0

        while (index < encoded.length) {
            var shift = 0
            var result = 0
            var byte: Int
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            lat += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            shift = 0
            result = 0
            do {
                byte = encoded[index++].code - 63
                result = result or ((byte and 0x1f) shl shift)
                shift += 5
            } while (byte >= 0x20)
            lng += if ((result and 1) != 0) (result shr 1).inv() else result shr 1

            poly += Coordinate(lat / 1E5, lng / 1E5)
        }
        return poly
    }

    private fun normalizeLocationQuery(raw: String): String {
        val trimmed = raw.trim()
        val lowercase = trimmed.lowercase()
        return when {
            "bangladesh" in lowercase -> trimmed
            "dhaka" in lowercase || "chattogram" in lowercase || "chittagong" in lowercase -> "$trimmed, Bangladesh"
            else -> "$trimmed, Dhaka, Bangladesh"
        }
    }

    private fun resolveDepartureTime(raw: String): LocalDateTime {
        val parsed = LocalTime.parse(raw.padStart(5, '0'), TimeFormatter)
        return LocalDateTime.now().withHour(parsed.hour).withMinute(parsed.minute).withSecond(0).withNano(0)
    }

    private fun riskLevel(riskPercent: Int): String = when {
        riskPercent >= 70 -> "High"
        riskPercent >= 45 -> "Medium"
        else -> "Low"
    }

    private fun Double.roundKey(): String = "%.3f".format(this)

    private data class SamplePoint(
        val label: String,
        val lat: Double,
        val lng: Double
    )

    private data class ForecastMatch(
        val rainMm: Double,
        val label: String
    )

    private data class PointRisk(
        val label: String,
        val lat: Double,
        val lng: Double,
        val rainMm: Double,
        val forecastTime: String,
        val riskPercent: Int,
        val riskLevel: String
    )

    private data class RouteCandidate(
        val name: String,
        val riskPercent: Int,
        val recommendedTime: String,
        val distanceText: String,
        val durationText: String,
        val riskySections: List<RouteRiskSection>
    )

    private companion object {
        val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
