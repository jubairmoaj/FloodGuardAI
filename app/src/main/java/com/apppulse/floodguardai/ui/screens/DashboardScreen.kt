package com.apppulse.floodguardai.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apppulse.floodguardai.data.model.LocationInput
import com.apppulse.floodguardai.ui.theme.Cyan400
import com.apppulse.floodguardai.ui.theme.Navy700
import com.apppulse.floodguardai.ui.theme.Navy800
import com.apppulse.floodguardai.ui.theme.OnDark
import com.apppulse.floodguardai.ui.theme.OnDarkMuted
import com.apppulse.floodguardai.ui.theme.RiskAmber
import com.apppulse.floodguardai.ui.theme.RiskGreen
import com.apppulse.floodguardai.ui.theme.RiskRed
import com.apppulse.floodguardai.ui.viewmodel.MainViewModel
import com.google.android.gms.location.LocationServices
import android.content.pm.PackageManager
import androidx.compose.ui.platform.LocalContext

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val state by viewModel.dashboardState.collectAsStateWithLifecycle()
    var latText by remember { mutableStateOf("23.8103") }
    var lngText by remember { mutableStateOf("90.4125") }
    val locationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            locationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.refreshDashboard(
                        LocationInput(lat = location.latitude, lng = location.longitude, name = "Current GPS")
                    )
                    latText = location.latitude.toString()
                    lngText = location.longitude.toString()
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Gradient header ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Navy700, MaterialTheme.colorScheme.background)
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    text = "Flood Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = Cyan400,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.padding(start = 4.dp))
                    Text(
                        text = state.locationLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnDarkMuted
                    )
                }
            }
        }

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Cyan400)
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // ── Risk level featured card ──────────────────────────────────
                val riskLevel = state.prediction?.riskLevel ?: "Unknown"
                val riskColor = when {
                    riskLevel.contains("High", ignoreCase = true)   -> RiskRed
                    riskLevel.contains("Medium", ignoreCase = true) -> RiskAmber
                    riskLevel.contains("Low", ignoreCase = true)    -> RiskGreen
                    else                                            -> OnDarkMuted
                }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = Navy800),
                    elevation = CardDefaults.elevatedCardElevation(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(riskColor.copy(alpha = 0.25f), Color.Transparent)))
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Current Risk Level", style = MaterialTheme.typography.labelLarge, color = OnDarkMuted)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(riskLevel, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = riskColor)
                            }
                            Text(
                                text = "${state.prediction?.floodProbability ?: 0}%",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = riskColor
                            )
                        }
                    }
                }

                // ── Stat cards row 1 ─────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(modifier = Modifier.weight(1f), title = "Rain Forecast", value = "${"%.1f".format(state.rainMmHour)} mm/h")
                    StatCard(modifier = Modifier.weight(1f), title = "Peak Risk Time", value = state.prediction?.peakRiskTime ?: "--")
                }

                // ── Confidence card ───────────────────────────────────────────
                StatCard(
                    modifier = Modifier.fillMaxWidth(),
                    title = "AI Confidence",
                    value = "${state.prediction?.confidence ?: 0}%",
                    accentColor = Cyan400
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ── Location input ────────────────────────────────────────────
                Text("Update Location", style = MaterialTheme.typography.titleSmall, color = OnDarkMuted)
                OutlinedTextField(
                    value = latText,
                    onValueChange = { latText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Latitude") },
                    colors = floodTextFieldColors()
                )
                OutlinedTextField(
                    value = lngText,
                    onValueChange = { lngText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Longitude") },
                    colors = floodTextFieldColors()
                )

                // ── Action buttons ────────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                locationClient.lastLocation.addOnSuccessListener { location ->
                                    if (location != null) {
                                        viewModel.refreshDashboard(
                                            LocationInput(lat = location.latitude, lng = location.longitude, name = "Current GPS")
                                        )
                                        latText = location.latitude.toString()
                                        lngText = location.longitude.toString()
                                    }
                                }
                            } else {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = MaterialTheme.colorScheme.background)
                    ) {
                        Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.padding(start = 4.dp))
                        Text("Use GPS")
                    }
                    Button(
                        onClick = { viewModel.sendMessage("Will my area flood today?") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = Cyan400)
                    ) {
                        Icon(Icons.Outlined.SmartToy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.padding(start = 4.dp))
                        Text("Ask AI")
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val lat = latText.toDoubleOrNull() ?: return@Button
                            val lng = lngText.toDoubleOrNull() ?: return@Button
                            viewModel.refreshDashboard(LocationInput(lat = lat, lng = lng, name = "Manual location"))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = OnDark)
                    ) {
                        Text("Set Manual")
                    }
                    Button(
                        onClick = { viewModel.refreshMap() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = OnDark)
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.padding(start = 4.dp))
                        Text("Refresh Map")
                    }
                }

                state.error?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = RiskRed.copy(alpha = 0.15f))
                    ) {
                        Text(text = it, color = RiskRed, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    accentColor: Color = OnDark
) {
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(containerColor = Navy800),
        elevation = CardDefaults.elevatedCardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = OnDarkMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, color = accentColor)
        }
    }
}

@Composable
internal fun floodTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Cyan400,
    unfocusedBorderColor = Navy700,
    focusedLabelColor    = Cyan400,
    cursorColor          = Cyan400,
    focusedTextColor     = OnDark,
    unfocusedTextColor   = OnDark,
    unfocusedLabelColor  = OnDarkMuted
)
