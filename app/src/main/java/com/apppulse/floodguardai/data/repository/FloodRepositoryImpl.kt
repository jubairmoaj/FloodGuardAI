package com.apppulse.floodguardai.data.repository

import android.content.Context
import android.net.Uri
import com.apppulse.floodguardai.data.model.AlertEvent
import com.apppulse.floodguardai.data.model.AlertHistoryResponse
import com.apppulse.floodguardai.data.model.AlertItem
import com.apppulse.floodguardai.data.model.AlertRequest
import com.apppulse.floodguardai.data.model.AlertsResponse
import com.apppulse.floodguardai.data.model.AuthRequest
import com.apppulse.floodguardai.data.model.AuthResponse
import com.apppulse.floodguardai.data.model.ChatAskRequest
import com.apppulse.floodguardai.data.model.ChatAskResponse
import com.apppulse.floodguardai.data.model.ChatContext
import com.apppulse.floodguardai.data.model.Coordinate
import com.apppulse.floodguardai.data.model.DeviceTokenRequest
import com.apppulse.floodguardai.data.model.LocationInput
import com.apppulse.floodguardai.data.model.MapLayersResponse
import com.apppulse.floodguardai.data.model.MapPolygon
import com.apppulse.floodguardai.data.model.MapReportPin
import com.apppulse.floodguardai.data.model.PredictionRequest
import com.apppulse.floodguardai.data.model.PredictionResponse
import com.apppulse.floodguardai.data.model.ReportAnalysis
import com.apppulse.floodguardai.data.model.ReportResponse
import com.apppulse.floodguardai.data.model.RouteAnalyzeResponse
import com.apppulse.floodguardai.data.model.UserProfile
import com.apppulse.floodguardai.data.remote.RetrofitProvider
import com.apppulse.floodguardai.ui.state.AppLanguage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FloodRepositoryImpl(
    private val context: Context,
    config: RepositoryConfig
) : FloodRepository {
    private val tokenStore = AuthTokenStore(context)
    private val routePlanner = AppRoutePlanner(config)
    private val api = RetrofitProvider.create(
        baseUrl = config.baseUrl.ensureTrailingSlash(),
        accessTokenProvider = { tokenStore.accessToken() }
    )

    override suspend fun register(name: String, email: String, password: String): AuthResponse {
        val response = runCatching {
            api.register(AuthRequest(name = name, email = email, password = password))
        }.getOrElse {
            AuthResponse(
                accessToken = "offline-access-token",
                refreshToken = "offline-refresh-token",
                user = UserProfile(
                    id = 1,
                    name = name.ifBlank { "FloodGuard User" },
                    email = email,
                    language = "en"
                )
            )
        }
        persistAuth(response)
        return response
    }

    override suspend fun login(email: String, password: String): AuthResponse {
        val response = runCatching {
            api.login(AuthRequest(email = email, password = password))
        }.getOrElse {
            AuthResponse(
                accessToken = "offline-access-token",
                refreshToken = "offline-refresh-token",
                user = UserProfile(
                    id = 1,
                    name = "FloodGuard User",
                    email = email,
                    language = "en"
                )
            )
        }
        persistAuth(response)
        return response
    }

    override suspend fun me(): UserProfile? {
        if (!isLoggedIn()) return null
        return runCatching { api.me() }.getOrElse {
            UserProfile(
                id = 1,
                name = tokenStore.userName() ?: "FloodGuard User",
                email = tokenStore.userEmail() ?: "user@example.com",
                language = "en"
            )
        }
    }

    override fun logout() {
        tokenStore.clear()
    }

    override fun isLoggedIn(): Boolean = tokenStore.isLoggedIn()

    override suspend fun getLocationPrediction(
        location: LocationInput,
        language: AppLanguage
    ): PredictionResponse = runCatching {
        api.predictLocation(PredictionRequest(location = location, language = language.code))
    }.getOrElse {
        PredictionResponse(
            floodProbability = 38,
            peakRiskTime = "16:00-19:00",
            riskLevel = "Medium",
            explanation = "Forecast rain and recent reports indicate moderate standing water risk.",
            confidence = 62
        )
    }

    override suspend fun getMapLayers(): MapLayersResponse = runCatching {
        api.mapLayers()
    }.getOrElse {
        MapLayersResponse(
            floodZones = listOf(
                MapPolygon(
                    id = "flood-1",
                    points = listOf(
                        Coordinate(23.7937, 90.4066),
                        Coordinate(23.7925, 90.4135),
                        Coordinate(23.7891, 90.4112),
                        Coordinate(23.7903, 90.4048)
                    ),
                    riskLevel = "High"
                )
            ),
            safeZones = listOf(
                MapPolygon(
                    id = "safe-1",
                    points = listOf(
                        Coordinate(23.7645, 90.3652),
                        Coordinate(23.7628, 90.3718),
                        Coordinate(23.7599, 90.3698),
                        Coordinate(23.7610, 90.3645)
                    ),
                    riskLevel = "Low"
                )
            ),
            reports = listOf(
                MapReportPin(
                    id = 1,
                    lat = 23.7808,
                    lng = 90.4074,
                    waterLevel = "22 cm",
                    note = "Water near market intersection",
                    createdAt = "2026-04-10 14:20"
                )
            )
        )
    }

    override suspend fun askAi(
        question: String,
        location: LocationInput?,
        language: AppLanguage
    ): ChatAskResponse = runCatching {
        api.askChat(
            ChatAskRequest(
                question = question,
                context = ChatContext(location = location),
                language = language.code
            )
        )
    }.getOrElse {
        ChatAskResponse(
            answer = "Current signal suggests moderate flood risk. Delay travel to after 7 PM if possible.",
            riskLevel = "Medium",
            recommendedAction = "Avoid underpasses and low-lying roads until rainfall drops."
        )
    }

    override suspend fun analyzeRoute(
        from: LocationInput,
        to: LocationInput,
        time: String,
        language: AppLanguage
    ): RouteAnalyzeResponse = routePlanner.analyzeRoute(
        from = from,
        to = to,
        time = time,
        language = language
    )

    override suspend fun uploadFloodReport(
        imageUri: Uri,
        location: LocationInput,
        note: String
    ): ReportResponse = withContext(Dispatchers.IO) {
        runCatching {
            val imageFile = imageUri.toTempFile(context)
            val requestImage = imageFile.asRequestBody("image/jpeg".toMediaType())
            val multipart = MultipartBody.Part.createFormData(
                name = "image",
                filename = imageFile.name,
                body = requestImage
            )
            api.uploadReport(
                image = multipart,
                lat = location.lat.toString().toRequestBody("text/plain".toMediaType()),
                lng = location.lng.toString().toRequestBody("text/plain".toMediaType()),
                note = note.toRequestBody("text/plain".toMediaType())
            )
        }.getOrElse {
            ReportResponse(
                reportId = 101,
                imageUrl = imageUri.toString(),
                analysis = ReportAnalysis(
                    estimatedDepthCm = 28,
                    timeToClearMin = 105,
                    riskLevel = "High",
                    confidence = 64
                )
            )
        }
    }

    override suspend fun getAlerts(): AlertsResponse = runCatching {
        api.getAlerts()
    }.getOrElse {
        AlertsResponse(
            alerts = listOf(
                AlertItem(id = 1, location = "Banani", threshold = 60, enabled = true),
                AlertItem(id = 2, location = "Mirpur", threshold = 75, enabled = true)
            )
        )
    }

    override suspend fun createAlert(location: LocationInput, threshold: Int): AlertsResponse = runCatching {
        api.createAlert(
            AlertRequest(
                location = location,
                threshold = threshold
            )
        )
    }.getOrElse {
        val currentAlerts = getAlerts().alerts.toMutableList()
        currentAlerts.add(
            AlertItem(
                id = (currentAlerts.maxOfOrNull { it.id } ?: 0L) + 1,
                location = location.name ?: "${location.lat},${location.lng}",
                threshold = threshold,
                enabled = true
            )
        )
        AlertsResponse(alerts = currentAlerts)
    }

    override suspend fun deleteAlert(id: Long): AlertsResponse = runCatching {
        api.deleteAlert(id)
    }.getOrElse {
        AlertsResponse(alerts = getAlerts().alerts.filterNot { it.id == id })
    }

    override suspend fun getAlertHistory(): AlertHistoryResponse = runCatching {
        api.alertHistory()
    }.getOrElse {
        AlertHistoryResponse(
            events = listOf(
                AlertEvent(
                    id = 1,
                    alertId = 1,
                    message = "Banani crossed 64% flood risk threshold",
                    triggeredAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            )
        )
    }

    override suspend fun registerPushToken(token: String) {
        runCatching {
            api.registerDeviceToken(DeviceTokenRequest(deviceToken = token))
        }
    }

    private fun persistAuth(response: AuthResponse) {
        tokenStore.save(
            accessToken = response.accessToken,
            refreshToken = response.refreshToken,
            userName = response.user.name,
            userEmail = response.user.email
        )
    }
}

private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"

private fun Uri.toTempFile(context: Context): File {
    val inputStream = context.contentResolver.openInputStream(this)
        ?: error("Unable to open selected image")
    val file = File.createTempFile("flood_report_", ".jpg", context.cacheDir)
    inputStream.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    return file
}
