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
            config = RepositoryConfig(baseUrl = BuildConfig.API_BASE_URL)
        )
        val viewModel = MainViewModel(repository = repository)

        setContent {
            FloodGuardTheme {
                FloodGuardApp(viewModel = viewModel)
            }
        }
    }
}
