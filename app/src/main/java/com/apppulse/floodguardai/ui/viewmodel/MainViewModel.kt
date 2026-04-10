package com.apppulse.floodguardai.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apppulse.floodguardai.data.model.LocationInput
import com.apppulse.floodguardai.data.repository.FloodRepository
import com.apppulse.floodguardai.ui.state.AlertsUiState
import com.apppulse.floodguardai.ui.state.AppLanguage
import com.apppulse.floodguardai.ui.state.ChatMessage
import com.apppulse.floodguardai.ui.state.ChatUiState
import com.apppulse.floodguardai.ui.state.DashboardUiState
import com.apppulse.floodguardai.ui.state.MapUiState
import com.apppulse.floodguardai.ui.state.ReportUiState
import com.apppulse.floodguardai.ui.state.RouteUiState
import com.apppulse.floodguardai.ui.state.SettingsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: FloodRepository
) : ViewModel() {
    private val defaultLocation = LocationInput(lat = 23.8103, lng = 90.4125, name = "Dhaka")

    private val _dashboardState = MutableStateFlow(DashboardUiState(isLoading = true))
    val dashboardState: StateFlow<DashboardUiState> = _dashboardState.asStateFlow()

    private val _mapState = MutableStateFlow(MapUiState(isLoading = true))
    val mapState: StateFlow<MapUiState> = _mapState.asStateFlow()

    private val _chatState = MutableStateFlow(
        ChatUiState(
            messages = listOf(
                ChatMessage(
                    fromUser = false,
                    text = "Ask anything about flood risk, travel safety, or weather impact."
                )
            )
        )
    )
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    private val _routeState = MutableStateFlow(RouteUiState())
    val routeState: StateFlow<RouteUiState> = _routeState.asStateFlow()

    private val _reportState = MutableStateFlow(ReportUiState())
    val reportState: StateFlow<ReportUiState> = _reportState.asStateFlow()

    private val _alertsState = MutableStateFlow(AlertsUiState(isLoading = true))
    val alertsState: StateFlow<AlertsUiState> = _alertsState.asStateFlow()

    private val _settingsState = MutableStateFlow(SettingsUiState())
    val settingsState: StateFlow<SettingsUiState> = _settingsState.asStateFlow()

    init {
        syncAuthState()
        refreshDashboard(defaultLocation)
        refreshMap()
        if (repository.isLoggedIn()) {
            refreshAlerts()
        } else {
            _alertsState.value = AlertsUiState(isLoading = false)
        }
    }

    fun refreshDashboard(location: LocationInput = defaultLocation) {
        _dashboardState.value = _dashboardState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                repository.getLocationPrediction(location, _settingsState.value.language)
            }.onSuccess { prediction ->
                _dashboardState.value = DashboardUiState(
                    isLoading = false,
                    locationLabel = location.name ?: "${location.lat}, ${location.lng}",
                    rainMmHour = (prediction.floodProbability / 5.2).coerceAtMost(30.0),
                    prediction = prediction
                )
            }.onFailure { error ->
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Unable to load dashboard."
                )
            }
        }
    }

    fun refreshMap() {
        _mapState.value = _mapState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.getMapLayers() }
                .onSuccess { layers ->
                    _mapState.value = MapUiState(isLoading = false, layers = layers)
                }
                .onFailure { error ->
                    _mapState.value = MapUiState(
                        isLoading = false,
                        error = error.message ?: "Unable to load map layers."
                    )
                }
        }
    }

    fun sendMessage(prompt: String) {
        if (prompt.isBlank()) return

        _chatState.value = _chatState.value.copy(
            isLoading = true,
            messages = _chatState.value.messages + ChatMessage(fromUser = true, text = prompt),
            error = null
        )
        viewModelScope.launch {
            runCatching {
                repository.askAi(
                    question = prompt,
                    location = defaultLocation,
                    language = _settingsState.value.language
                )
            }.onSuccess { response ->
                _chatState.value = _chatState.value.copy(
                    isLoading = false,
                    latestStructured = response,
                    messages = _chatState.value.messages + ChatMessage(
                        fromUser = false,
                        text = response.answer
                    )
                )
            }.onFailure { error ->
                _chatState.value = _chatState.value.copy(
                    isLoading = false,
                    error = error.message ?: "AI service failed."
                )
            }
        }
    }

    fun setRouteFrom(value: String) {
        _routeState.value = _routeState.value.copy(fromText = value)
    }

    fun setRouteTo(value: String) {
        _routeState.value = _routeState.value.copy(toText = value)
    }

    fun setRouteTime(value: String) {
        _routeState.value = _routeState.value.copy(timeText = value)
    }

    fun analyzeRoute() {
        val state = _routeState.value
        if (state.fromText.isBlank() || state.toText.isBlank()) {
            _routeState.value = state.copy(error = "From and To locations are required.")
            return
        }
        if (Regex("""^\d{1,2}:\d{2}$""").matches(state.timeText).not()) {
            _routeState.value = state.copy(error = "Time must be in HH:mm format.")
            return
        }

        _routeState.value = state.copy(isLoading = true, error = null, response = null)
        viewModelScope.launch {
            runCatching {
                repository.analyzeRoute(
                    from = LocationInput(0.0, 0.0, state.fromText),
                    to = LocationInput(0.0, 0.0, state.toText),
                    time = state.timeText,
                    language = _settingsState.value.language
                )
            }.onSuccess { response ->
                _routeState.value = _routeState.value.copy(isLoading = false, response = response)
            }.onFailure { error ->
                _routeState.value = _routeState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Route analysis failed."
                )
            }
        }
    }

    fun setReportNote(value: String) {
        _reportState.value = _reportState.value.copy(note = value)
    }

    fun setReportImage(uri: Uri?) {
        _reportState.value = _reportState.value.copy(imageUri = uri?.toString())
    }

    fun submitReport() {
        val current = _reportState.value
        val imageUri = current.imageUri ?: run {
            _reportState.value = current.copy(error = "Please upload an image first.")
            return
        }
        _reportState.value = current.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                repository.uploadFloodReport(
                    imageUri = Uri.parse(imageUri),
                    location = defaultLocation,
                    note = current.note
                )
            }.onSuccess { response ->
                _reportState.value = _reportState.value.copy(
                    isLoading = false,
                    lastAnalysis = response.analysis
                )
                refreshMap()
            }.onFailure { error ->
                _reportState.value = _reportState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Report upload failed."
                )
            }
        }
    }

    fun setAlertThreshold(value: Int) {
        _alertsState.value = _alertsState.value.copy(threshold = value.coerceIn(10, 95))
    }

    fun refreshAlerts() {
        if (!repository.isLoggedIn()) {
            _alertsState.value = AlertsUiState(isLoading = false, error = "Login required for alerts.")
            return
        }
        _alertsState.value = _alertsState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                repository.getAlerts() to repository.getAlertHistory()
            }.onSuccess { pair ->
                _alertsState.value = _alertsState.value.copy(
                    isLoading = false,
                    alerts = pair.first.alerts,
                    events = pair.second.events
                )
            }.onFailure { error ->
                _alertsState.value = _alertsState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Unable to load alerts."
                )
            }
        }
    }

    fun createAlert(locationName: String) {
        if (!repository.isLoggedIn()) {
            _alertsState.value = _alertsState.value.copy(error = "Please login to create alerts.")
            return
        }
        if (locationName.isBlank()) return
        _alertsState.value = _alertsState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching {
                repository.createAlert(
                    location = LocationInput(23.8103, 90.4125, locationName),
                    threshold = _alertsState.value.threshold
                )
            }.onSuccess { alerts ->
                _alertsState.value = _alertsState.value.copy(
                    isLoading = false,
                    alerts = alerts.alerts
                )
                refreshAlerts()
            }.onFailure { error ->
                _alertsState.value = _alertsState.value.copy(
                    isLoading = false,
                    error = error.message ?: "Unable to create alert."
                )
            }
        }
    }

    fun deleteAlert(id: Long) {
        if (!repository.isLoggedIn()) {
            _alertsState.value = _alertsState.value.copy(error = "Please login to delete alerts.")
            return
        }
        _alertsState.value = _alertsState.value.copy(isLoading = true, error = null)
        viewModelScope.launch {
            runCatching { repository.deleteAlert(id) }
                .onSuccess { alerts ->
                    _alertsState.value = _alertsState.value.copy(
                        isLoading = false,
                        alerts = alerts.alerts
                    )
                }
                .onFailure { error ->
                    _alertsState.value = _alertsState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to delete alert."
                    )
                }
        }
    }

    fun setLanguage(language: AppLanguage) {
        _settingsState.value = _settingsState.value.copy(language = language)
        refreshDashboard()
    }

    fun setNotifications(enabled: Boolean) {
        _settingsState.value = _settingsState.value.copy(notificationsEnabled = enabled)
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _settingsState.value = _settingsState.value.copy(authError = "Email and password are required.")
            return
        }
        _settingsState.value = _settingsState.value.copy(isAuthLoading = true, authError = null)
        viewModelScope.launch {
            runCatching { repository.login(email, password) }
                .onSuccess { auth ->
                    _settingsState.value = _settingsState.value.copy(
                        isAuthLoading = false,
                        isLoggedIn = true,
                        userName = auth.user.name,
                        userEmail = auth.user.email,
                        authError = null
                    )
                    refreshAlerts()
                }
                .onFailure { error ->
                    _settingsState.value = _settingsState.value.copy(
                        isAuthLoading = false,
                        authError = error.message ?: "Login failed."
                    )
                }
        }
    }

    fun register(name: String, email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _settingsState.value = _settingsState.value.copy(authError = "Email and password are required.")
            return
        }
        _settingsState.value = _settingsState.value.copy(isAuthLoading = true, authError = null)
        viewModelScope.launch {
            runCatching { repository.register(name.ifBlank { "FloodGuard User" }, email, password) }
                .onSuccess { auth ->
                    _settingsState.value = _settingsState.value.copy(
                        isAuthLoading = false,
                        isLoggedIn = true,
                        userName = auth.user.name,
                        userEmail = auth.user.email,
                        authError = null
                    )
                    refreshAlerts()
                }
                .onFailure { error ->
                    _settingsState.value = _settingsState.value.copy(
                        isAuthLoading = false,
                        authError = error.message ?: "Registration failed."
                    )
                }
        }
    }

    fun logout() {
        repository.logout()
        _settingsState.value = _settingsState.value.copy(
            isLoggedIn = false,
            userName = null,
            userEmail = null,
            authError = null
        )
        _alertsState.value = AlertsUiState(isLoading = false)
    }

    private fun syncAuthState() {
        val loggedIn = repository.isLoggedIn()
        _settingsState.value = _settingsState.value.copy(isLoggedIn = loggedIn)
        if (!loggedIn) {
            return
        }
        viewModelScope.launch {
            runCatching { repository.me() }
                .onSuccess { user ->
                    _settingsState.value = _settingsState.value.copy(
                        isLoggedIn = user != null,
                        userName = user?.name,
                        userEmail = user?.email
                    )
                }
                .onFailure {
                    _settingsState.value = _settingsState.value.copy(
                        isLoggedIn = false,
                        userName = null,
                        userEmail = null
                    )
                }
        }
    }
}
