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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apppulse.floodguardai.ui.state.AppLanguage
import com.apppulse.floodguardai.ui.theme.Cyan400
import com.apppulse.floodguardai.ui.theme.Navy700
import com.apppulse.floodguardai.ui.theme.Navy800
import com.apppulse.floodguardai.ui.theme.OnDark
import com.apppulse.floodguardai.ui.theme.OnDarkMuted
import com.apppulse.floodguardai.ui.theme.RiskRed
import com.apppulse.floodguardai.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val state by viewModel.settingsState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
                .background(Brush.verticalGradient(listOf(Navy700, MaterialTheme.colorScheme.background)))
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = OnDark)
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Profile section ───────────────────────────────────────────────
            SectionCard(icon = Icons.Outlined.AccountCircle, title = "Profile") {
                if (state.isLoggedIn) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(Cyan400, Navy700))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (state.userName?.firstOrNull() ?: "U").toString().uppercase(),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = OnDark
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(state.userName ?: "User", style = MaterialTheme.typography.titleSmall, color = OnDark, fontWeight = FontWeight.SemiBold)
                            Text(state.userEmail ?: "", style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = viewModel::logout,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RiskRed.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Outlined.Logout, contentDescription = null, tint = RiskRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.padding(start = 6.dp))
                        Text("Logout", color = RiskRed)
                    }
                } else {
                    Text("Sign in to access all features", style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = floodTextFieldColors()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = floodTextFieldColors()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation(),
                        colors = floodTextFieldColors()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { viewModel.login(email, password) },
                            enabled = !state.isAuthLoading,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan400, contentColor = Navy800),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(if (state.isAuthLoading) "…" else "Login", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = { viewModel.register(name, email, password) },
                            enabled = !state.isAuthLoading,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Navy700, contentColor = OnDark),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Register")
                        }
                    }
                    state.authError?.let {
                        Text(it, color = RiskRed, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // ── Language section ──────────────────────────────────────────────
            SectionCard(icon = Icons.Outlined.Language, title = "Language") {
                AppLanguage.entries.forEach { lang ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = when (lang) {
                                AppLanguage.English -> "English"
                                AppLanguage.Bangla  -> "বাংলা (Bangla)"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.language == lang) Cyan400 else OnDark
                        )
                        RadioButton(
                            selected = state.language == lang,
                            onClick = { viewModel.setLanguage(lang) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Cyan400,
                                unselectedColor = OnDarkMuted
                            )
                        )
                    }
                    if (lang != AppLanguage.entries.last()) {
                        Divider(color = Navy700, thickness = 0.5.dp)
                    }
                }
            }

            // ── Preferences section ───────────────────────────────────────────
            SectionCard(icon = Icons.Outlined.Notifications, title = "Preferences") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Push Notifications", style = MaterialTheme.typography.bodyMedium, color = OnDark)
                        Text("Alert when flood risk rises", style = MaterialTheme.typography.labelSmall, color = OnDarkMuted)
                    }
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = viewModel::setNotifications,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Navy800,
                            checkedTrackColor = Cyan400,
                            uncheckedThumbColor = OnDarkMuted,
                            uncheckedTrackColor = Navy700
                        )
                    )
                }
            }

            // ── Saved locations section ───────────────────────────────────────
            SectionCard(icon = Icons.Outlined.LocationOn, title = "Saved Locations") {
                if (state.savedLocations.isEmpty()) {
                    Text("No saved locations.", style = MaterialTheme.typography.bodySmall, color = OnDarkMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.savedLocations.forEachIndexed { idx, location ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Cyan400)
                                )
                                Text(location, style = MaterialTheme.typography.bodyMedium, color = OnDark)
                            }
                            if (idx < state.savedLocations.lastIndex) {
                                Divider(color = Navy700, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionCard(
    icon: ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Navy800),
        elevation = CardDefaults.elevatedCardElevation(3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, contentDescription = null, tint = Cyan400, modifier = Modifier.size(20.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, color = OnDark, fontWeight = FontWeight.Bold)
            }
            Divider(color = Navy700, thickness = 1.dp)
            content()
        }
    }
}
