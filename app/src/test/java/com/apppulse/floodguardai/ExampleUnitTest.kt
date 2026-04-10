package com.apppulse.floodguardai

import android.net.Uri
import com.apppulse.floodguardai.data.model.AlertHistoryResponse
import com.apppulse.floodguardai.data.model.AlertItem
import com.apppulse.floodguardai.data.model.AlertsResponse
import com.apppulse.floodguardai.data.model.AuthResponse
import com.apppulse.floodguardai.data.model.ChatAskResponse
import com.apppulse.floodguardai.data.model.LocationInput
import com.apppulse.floodguardai.data.model.MapLayersResponse
import com.apppulse.floodguardai.data.model.PredictionResponse
import com.apppulse.floodguardai.data.model.ReportAnalysis
import com.apppulse.floodguardai.data.model.ReportResponse
import com.apppulse.floodguardai.data.model.RouteAnalyzeResponse
import com.apppulse.floodguardai.data.model.UserProfile
import com.apppulse.floodguardai.data.repository.FloodRepository
import com.apppulse.floodguardai.ui.state.AppLanguage
import com.apppulse.floodguardai.ui.viewmodel.MainViewModel
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Test
import org.junit.After
import org.junit.Before
import java.lang.IllegalStateException

@OptIn(ExperimentalCoroutinesApi::class)
class ExampleUnitTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun dashboard_refresh_populates_prediction() = runTest {
        val viewModel = MainViewModel(FakeRepository())
        advanceUntilIdle()

        val state = viewModel.dashboardState.value
        assertThat(state.prediction?.floodProbability).isEqualTo(51)
        assertThat(state.prediction?.riskLevel).isEqualTo("Medium")
    }

    @Test
    fun route_analyze_requires_from_and_to() = runTest {
        val viewModel = MainViewModel(FakeRepository())
        viewModel.analyzeRoute()
        advanceUntilIdle()

        assertThat(viewModel.routeState.value.error).isEqualTo("From and To locations are required.")
    }

    @Test
    fun submit_report_without_image_returns_error() = runTest {
        val viewModel = MainViewModel(FakeRepository())
        viewModel.submitReport()
        advanceUntilIdle()

        assertThat(viewModel.reportState.value.error).isEqualTo("Please upload an image first.")
    }

    @Test
    fun ai_message_appends_assistant_response() = runTest {
        val viewModel = MainViewModel(FakeRepository())
        viewModel.sendMessage("Will my area flood today?")
        advanceUntilIdle()

        val messages = viewModel.chatState.value.messages
        assertThat(messages.last().fromUser).isFalse()
        assertThat(messages.last().text).contains("moderate")
    }
}

private class FakeRepository(
    private val shouldFailRoute: Boolean = false
) : FloodRepository {
    override suspend fun register(name: String, email: String, password: String): AuthResponse {
        return AuthResponse(
            accessToken = "token",
            refreshToken = "refresh",
            user = UserProfile(
                id = 1,
                name = name,
                email = email,
                language = "en"
            )
        )
    }

    override suspend fun login(email: String, password: String): AuthResponse {
        return AuthResponse(
            accessToken = "token",
            refreshToken = "refresh",
            user = UserProfile(
                id = 1,
                name = "Test User",
                email = email,
                language = "en"
            )
        )
    }

    override suspend fun me(): UserProfile? {
        return UserProfile(id = 1, name = "Test User", email = "test@example.com")
    }

    override fun logout() = Unit

    override fun isLoggedIn(): Boolean = true

    override suspend fun getLocationPrediction(
        location: LocationInput,
        language: AppLanguage
    ): PredictionResponse {
        return PredictionResponse(
            floodProbability = 51,
            peakRiskTime = "16:00-18:00",
            riskLevel = "Medium",
            explanation = "rain + history",
            confidence = 71
        )
    }

    override suspend fun getMapLayers(): MapLayersResponse {
        return MapLayersResponse(
            floodZones = emptyList(),
            safeZones = emptyList(),
            reports = emptyList()
        )
    }

    override suspend fun askAi(
        question: String,
        location: LocationInput?,
        language: AppLanguage
    ): ChatAskResponse {
        return ChatAskResponse(
            answer = "Current signal suggests moderate flood risk.",
            riskLevel = "Medium",
            recommendedAction = "Delay travel if possible."
        )
    }

    override suspend fun analyzeRoute(
        from: LocationInput,
        to: LocationInput,
        time: String,
        language: AppLanguage
    ): RouteAnalyzeResponse {
        if (shouldFailRoute) throw IllegalStateException("route failed")
        return RouteAnalyzeResponse(
            decision = "Safe",
            riskPercent = 33,
            recommendedTime = time,
            recommendedAction = "Proceed carefully",
            alternatives = emptyList()
        )
    }

    override suspend fun uploadFloodReport(
        imageUri: Uri,
        location: LocationInput,
        note: String
    ): ReportResponse {
        return ReportResponse(
            reportId = 10,
            imageUrl = imageUri.toString(),
            analysis = ReportAnalysis(
                estimatedDepthCm = 14,
                timeToClearMin = 50,
                riskLevel = "Medium",
                confidence = 60
            )
        )
    }

    override suspend fun getAlerts(): AlertsResponse {
        return AlertsResponse(
            alerts = listOf(
                AlertItem(id = 1, location = "Banani", threshold = 60, enabled = true)
            )
        )
    }

    override suspend fun createAlert(location: LocationInput, threshold: Int): AlertsResponse {
        return getAlerts()
    }

    override suspend fun deleteAlert(id: Long): AlertsResponse {
        return AlertsResponse(alerts = emptyList())
    }

    override suspend fun getAlertHistory(): AlertHistoryResponse {
        return AlertHistoryResponse(events = emptyList())
    }

    override suspend fun registerPushToken(token: String) {
    }
}
