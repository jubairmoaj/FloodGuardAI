package com.apppulse.floodguardai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.apppulse.floodguardai.data.repository.FloodRepositoryImpl
import com.apppulse.floodguardai.data.repository.RepositoryConfig
import com.apppulse.floodguardai.ui.theme.FloodGuardTheme
import com.apppulse.floodguardai.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = FloodRepositoryImpl(
            context = applicationContext,
            config = RepositoryConfig(
                baseUrl = BuildConfig.API_BASE_URL,
                googleMapsRestBaseUrl = BuildConfig.GOOGLE_MAPS_REST_BASE_URL,
                googleMapsApiKey = BuildConfig.GOOGLE_MAPS_API_KEY,
                openWeatherBaseUrl = BuildConfig.OPENWEATHER_BASE_URL,
                openWeatherApiKey = BuildConfig.OPENWEATHER_API_KEY,
                geminiBaseUrl = BuildConfig.GEMINI_BASE_URL,
                geminiApiKey = BuildConfig.GEMINI_API_KEY,
                geminiModel = BuildConfig.GEMINI_MODEL
            )
        )
        val viewModel = MainViewModel(repository = repository)

        setContent {
            FloodGuardTheme {
                FloodGuardApp(viewModel = viewModel)
            }
        }
    }
}
