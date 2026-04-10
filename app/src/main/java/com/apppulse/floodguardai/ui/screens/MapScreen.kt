package com.apppulse.floodguardai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apppulse.floodguardai.ui.theme.Cyan400
import com.apppulse.floodguardai.ui.theme.Navy800
import com.apppulse.floodguardai.ui.theme.OnDark
import com.apppulse.floodguardai.ui.theme.OnDarkMuted
import com.apppulse.floodguardai.ui.theme.RiskGreen
import com.apppulse.floodguardai.ui.theme.RiskRed
import com.apppulse.floodguardai.ui.viewmodel.MainViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun MapScreen(viewModel: MainViewModel) {
    val state by viewModel.mapState.collectAsStateWithLifecycle()

    if (state.isLoading && state.layers == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Cyan400)
        }
        return
    }

    val layers = state.layers
    if (layers == null) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = state.error ?: "No map data available.",
                color = MaterialTheme.colorScheme.error
            )
        }
        return
    }

    val allPoints = (layers.floodZones + layers.safeZones).flatMap { zone ->
        zone.points.map { LatLng(it.lat, it.lng) }
    }
    val center = allPoints.firstOrNull() ?: LatLng(23.8103, 90.4125)
    val bounds = if (allPoints.isNotEmpty()) {
        LatLngBounds.builder().apply { allPoints.forEach { include(it) } }.build()
    } else null
    val cameraState = rememberCameraPositionState()

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(mapType = MapType.NORMAL),
            onMapLoaded = {
                if (bounds != null) {
                    cameraState.move(
                        com.google.android.gms.maps.CameraUpdateFactory.newLatLngBounds(bounds, 120)
                    )
                } else {
                    cameraState.position = CameraPosition.fromLatLngZoom(center, 12f)
                }
            }
        ) {
            layers.floodZones.forEach { zone ->
                Polygon(
                    points = zone.points.map { LatLng(it.lat, it.lng) },
                    fillColor = RiskRed.copy(alpha = 0.30f),
                    strokeColor = RiskRed,
                    strokeWidth = 4f
                )
            }

            layers.safeZones.forEach { zone ->
                Polygon(
                    points = zone.points.map { LatLng(it.lat, it.lng) },
                    fillColor = RiskGreen.copy(alpha = 0.25f),
                    strokeColor = RiskGreen,
                    strokeWidth = 4f
                )
            }

            layers.reports.forEach { report ->
                Marker(
                    state = MarkerState(LatLng(report.lat, report.lng)),
                    title = report.waterLevel ?: "Flood report",
                    snippet = report.note
                )
            }
        }

        // ── Floating legend ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Navy800.copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "MAP LEGEND",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnDarkMuted,
                    letterSpacing = 1.sp
                )
                LegendRow(color = RiskRed,   label = "Flood Zone")
                LegendRow(color = RiskGreen, label = "Safe Zone")
                LegendRow(color = Cyan400,   label = "User Report")
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, fontSize = 12.sp, color = OnDark)
    }
}
