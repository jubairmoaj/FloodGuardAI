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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun AlertsScreen(viewModel: MainViewModel) {
    val state by viewModel.alertsState.collectAsStateWithLifecycle()
    var newLocation by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Navy700, MaterialTheme.colorScheme.background)))
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Notifications, contentDescription = null, tint = Cyan400, modifier = Modifier.size(28.dp))
                    Column {
                        Text("Flood Alerts", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnDark)
                        Text("Get notified when risk crosses your threshold", style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
                    }
                }
            }
        }

        // ── Threshold section ─────────────────────────────────────────────────
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = Navy800),
                elevation = CardDefaults.elevatedCardElevation(4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Alert Threshold", style = MaterialTheme.typography.titleSmall, color = OnDark, fontWeight = FontWeight.SemiBold)
                        Box(
                            modifier = Modifier
                                .background(Cyan400.copy(alpha = 0.20f), shape = RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("${state.threshold}%", style = MaterialTheme.typography.labelLarge, color = Cyan400, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = state.threshold.toFloat(),
                        onValueChange = { viewModel.setAlertThreshold(it.toInt()) },
                        valueRange = 10f..95f,
                        steps = 16,
                        colors = SliderDefaults.colors(
                            thumbColor = Cyan400,
                            activeTrackColor = Cyan400,
                            inactiveTrackColor = Navy700
                        )
                    )
                    Text(
                        "Notify me when flood probability exceeds ${state.threshold}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = OnDarkMuted
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }

        // ── Add alert ─────────────────────────────────────────────────────────
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Add New Alert", style = MaterialTheme.typography.titleSmall, color = OnDarkMuted, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newLocation,
                        onValueChange = { newLocation = it },
                        label = { Text("Location name") },
                        modifier = Modifier.weight(1f),
                        colors = floodTextFieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            viewModel.createAlert(newLocation)
                            newLocation = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy800),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "Add", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // ── My Alerts list ────────────────────────────────────────────────────
        item {
            SectionHeader(title = "My Alerts", count = state.alerts.size)
        }

        if (state.alerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No alerts yet. Add a location above.", style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
                }
            }
        } else {
            items(state.alerts) { alert ->
                val thresholdColor = when {
                    alert.threshold >= 70 -> RiskRed
                    alert.threshold >= 40 -> RiskAmber
                    else -> RiskGreen
                }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Navy800),
                    elevation = CardDefaults.elevatedCardElevation(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(thresholdColor)
                            )
                            Column {
                                Text(alert.location, style = MaterialTheme.typography.titleSmall, color = OnDark, fontWeight = FontWeight.SemiBold)
                                Text("Threshold: ${alert.threshold}%", style = MaterialTheme.typography.labelSmall, color = OnDarkMuted)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteAlert(alert.id) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = RiskRed.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }

        // ── Alert History ─────────────────────────────────────────────────────
        item {
            SectionHeader(title = "Alert History", count = state.events.size)
        }

        if (state.events.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No alert history.", style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
                }
            }
        } else {
            items(state.events) { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(event.message, style = MaterialTheme.typography.bodySmall, color = OnDark, modifier = Modifier.weight(1f))
                    Text(event.triggeredAt, style = MaterialTheme.typography.labelSmall, color = OnDarkMuted, fontSize = 10.sp)
                }
                Divider(color = Navy700.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }

        item {
            state.error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = RiskRed.copy(alpha = 0.15f))
                ) {
                    Text(text = it, color = RiskRed, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = OnDark, fontWeight = FontWeight.Bold)
        if (count > 0) {
            Box(
                modifier = Modifier
                    .background(Navy700, shape = CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("$count", style = MaterialTheme.typography.labelSmall, color = OnDarkMuted)
            }
        }
    }
}
