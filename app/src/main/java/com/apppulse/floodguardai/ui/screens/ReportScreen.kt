package com.apppulse.floodguardai.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.UploadFile
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
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
fun ReportScreen(viewModel: MainViewModel) {
    val state by viewModel.reportState.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> viewModel.setReportImage(uri) }

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
                Text("Flood Report", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnDark)
                Text("Upload an image to get AI analysis", style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Image picker / preview ────────────────────────────────────────
            if (state.imageUri != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { launcher.launch("image/*") }
                ) {
                    AsyncImage(
                        model = state.imageUri,
                        contentDescription = "Selected flood image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, Navy800.copy(alpha = 0.8f))))
                            .padding(12.dp)
                    ) {
                        Text("Tap to change image", style = MaterialTheme.typography.labelSmall, color = OnDarkMuted)
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(
                            width = 2.dp,
                            brush = Brush.horizontalGradient(listOf(Cyan400.copy(alpha = 0.5f), Navy700)),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .background(Navy800)
                        .clickable { launcher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, tint = Cyan400, modifier = Modifier.size(48.dp))
                        Text("Tap to select flood image", style = MaterialTheme.typography.bodyMedium, color = Cyan400, fontWeight = FontWeight.SemiBold)
                        Text("JPG, PNG supported", style = MaterialTheme.typography.labelSmall, color = OnDarkMuted)
                    }
                }
            }

            // ── Note field ────────────────────────────────────────────────────
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::setReportNote,
                label = { Text("Add a note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                colors = floodTextFieldColors()
            )

            // ── Submit button ─────────────────────────────────────────────────
            Button(
                onClick = viewModel::submitReport,
                enabled = !state.isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy800),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Navy800, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.padding(start = 6.dp))
                    Text("Submit Report", fontWeight = FontWeight.SemiBold)
                }
            }

            // ── Analysis result card ──────────────────────────────────────────
            state.lastAnalysis?.let { analysis ->
                val riskColor = when {
                    analysis.riskLevel.contains("High", ignoreCase = true)   -> RiskRed
                    analysis.riskLevel.contains("Medium", ignoreCase = true) -> RiskAmber
                    else -> RiskGreen
                }
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(containerColor = Navy800),
                    elevation = CardDefaults.elevatedCardElevation(4.dp)
                ) {
                    Column {
                        // Risk badge header
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(riskColor.copy(alpha = 0.20f))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("AI Analysis Result", style = MaterialTheme.typography.titleSmall, color = OnDark, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .background(riskColor.copy(alpha = 0.30f), shape = RoundedCornerShape(20.dp))
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(analysis.riskLevel, style = MaterialTheme.typography.labelMedium, color = riskColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Divider(color = Navy700, thickness = 1.dp)

                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AnalysisRow(label = "Estimated Depth", value = "${analysis.estimatedDepthCm} cm")
                            AnalysisRow(label = "Time to Clear", value = "${analysis.timeToClearMin} min")
                            AnalysisRow(label = "AI Confidence", value = "${analysis.confidence}%", valueColor = Cyan400)
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
private fun AnalysisRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = OnDark
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}
