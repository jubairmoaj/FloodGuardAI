package com.apppulse.floodguardai.ui.screens

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
import androidx.compose.material.icons.outlined.AltRoute
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apppulse.floodguardai.ui.theme.Cyan400
import com.apppulse.floodguardai.ui.theme.Navy700
import com.apppulse.floodguardai.ui.theme.Navy800
import com.apppulse.floodguardai.ui.theme.OnDark
import com.apppulse.floodguardai.ui.theme.OnDarkMuted
import com.apppulse.floodguardai.ui.theme.RiskAmber
import com.apppulse.floodguardai.ui.theme.RiskGreen
import com.apppulse.floodguardai.ui.theme.RiskRed
import com.apppulse.floodguardai.ui.viewmodel.MainViewModel

@Composable
fun RoutePlannerScreen(viewModel: MainViewModel) {
    val state by viewModel.routeState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(colors = listOf(Navy700, MaterialTheme.colorScheme.background))
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column {
                Text(
                    "Route Safety Planner",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnDark
                )
                Text("Check if your route is flood-safe", style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Form card ─────────────────────────────────────────────────────
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = Navy800),
                elevation = CardDefaults.elevatedCardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = state.fromText,
                        onValueChange = viewModel::setRouteFrom,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("From") },
                        placeholder = { Text("e.g. Dhanmondi 27, Dhaka") },
                        colors = floodTextFieldColors()
                    )
                    OutlinedTextField(
                        value = state.toText,
                        onValueChange = viewModel::setRouteTo,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("To") },
                        placeholder = { Text("e.g. Bashundhara R/A, Dhaka") },
                        colors = floodTextFieldColors()
                    )
                    OutlinedTextField(
                        value = state.timeText,
                        onValueChange = viewModel::setRouteTime,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Departure Time (HH:mm)") },
                        colors = floodTextFieldColors()
                    )
                }
            }

            Button(
                onClick = viewModel::analyzeRoute,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy800),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Navy800, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.AltRoute, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.padding(start = 6.dp))
                    Text("Check Route Safety", fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Result card ───────────────────────────────────────────────────
            state.response?.let { response ->
                val decision = response.decision.lowercase()
                val (decisionColor, decisionIcon) = when {
                    decision.contains("go") || decision.contains("safe") -> RiskGreen to Icons.Outlined.CheckCircle
                    decision.contains("avoid") || decision.contains("danger") -> RiskRed to Icons.Outlined.Warning
                    else -> RiskAmber to Icons.Outlined.Warning
                }

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = Navy800),
                    elevation = CardDefaults.elevatedCardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Decision banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(decisionColor.copy(alpha = 0.30f), decisionColor.copy(alpha = 0.05f))
                                    )
                                )
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(decisionIcon, contentDescription = null, tint = decisionColor, modifier = Modifier.size(28.dp))
                                Column {
                                    Text("Decision", style = MaterialTheme.typography.labelMedium, color = OnDarkMuted)
                                    Text(response.decision, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = decisionColor)
                                }
                            }
                        }

                        Divider(color = Navy700, thickness = 1.dp)

                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            response.resolvedOrigin?.let { origin ->
                                RouteDetailRow(label = "Origin", value = origin.name)
                            }
                            response.resolvedDestination?.let { destination ->
                                RouteDetailRow(label = "Destination", value = destination.name)
                            }
                            if (response.chosenRoute.isNotBlank()) {
                                RouteDetailRow(label = "Chosen Route", value = response.chosenRoute)
                            }
                            RouteDetailRow(label = "Risk Level", value = "${response.riskPercent}%", valueColor = decisionColor)
                            RouteDetailRow(label = "Suggested Time", value = response.recommendedTime)
                            RouteDetailRow(label = "Recommended Action", value = response.recommendedAction)
                            if (response.explanation.isNotBlank()) {
                                Divider(color = Navy700, thickness = 1.dp)
                                Text(
                                    text = response.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = OnDarkMuted
                                )
                            }

                            if (response.riskySections.isNotEmpty()) {
                                Divider(color = Navy700, thickness = 1.dp)
                                Text(
                                    "Risky Sections",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = OnDarkMuted,
                                    fontWeight = FontWeight.SemiBold
                                )
                                response.riskySections.forEach { section ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (section.riskLevel.equals("High", ignoreCase = true)) {
                                                RiskRed.copy(alpha = 0.14f)
                                            } else {
                                                Navy700
                                            }
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    section.label,
                                                    color = OnDark,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    "${section.riskPercent}% ${section.riskLevel}",
                                                    color = if (section.riskLevel.equals("High", ignoreCase = true)) RiskRed else RiskAmber,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                "Rain ${"%.1f".format(section.rainMm)} mm near ${section.forecastTime}",
                                                color = OnDarkMuted,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                "History avg ${"%.1f".format(section.historySeverityAvg)} | Reports avg ${"%.1f".format(section.reportDepthAvgCm)} cm",
                                                color = OnDarkMuted,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }

                            if (response.alternatives.isNotEmpty()) {
                                Divider(color = Navy700, thickness = 1.dp)
                                Text("Alternative Routes", style = MaterialTheme.typography.titleSmall, color = OnDarkMuted, fontWeight = FontWeight.SemiBold)
                                response.alternatives.forEach { alt ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Navy700),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(alt.route, style = MaterialTheme.typography.bodySmall, color = OnDark, fontWeight = FontWeight.SemiBold)
                                                if (alt.reason.isNotBlank()) {
                                                    Text(
                                                        alt.reason,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = OnDarkMuted
                                                    )
                                                }
                                            }
                                            Text("${alt.riskPercent}% · ${alt.recommendedTime}", style = MaterialTheme.typography.labelSmall, color = OnDarkMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RouteDetailRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = OnDark
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}
