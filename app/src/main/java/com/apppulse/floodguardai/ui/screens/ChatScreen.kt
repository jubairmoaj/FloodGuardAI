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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apppulse.floodguardai.ui.theme.Cyan400
import com.apppulse.floodguardai.ui.theme.Navy700
import com.apppulse.floodguardai.ui.theme.Navy800
import com.apppulse.floodguardai.ui.theme.OnDark
import com.apppulse.floodguardai.ui.theme.OnDarkMuted
import com.apppulse.floodguardai.ui.theme.RiskAmber
import com.apppulse.floodguardai.ui.viewmodel.MainViewModel

@Composable
fun ChatScreen(viewModel: MainViewModel) {
    val state by viewModel.chatState.collectAsStateWithLifecycle()
    var prompt by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Header ────────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Navy800)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.SmartToy,
                contentDescription = null,
                tint = Cyan400,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.padding(start = 10.dp))
            Column {
                Text("Flood AI Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = OnDark)
                Text("Powered by Gemini", style = MaterialTheme.typography.labelSmall, color = OnDarkMuted)
            }
        }

        Divider(color = Navy700, thickness = 1.dp)

        // ── Messages ──────────────────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
            items(state.messages) { message ->
                if (message.fromUser) {
                    // ── User bubble (right, cyan) ─────────────────────────────
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .background(
                                    color = Cyan400,
                                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(text = message.text, color = Navy800, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    // ── AI bubble (left, surface variant) ────────────────────
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .background(
                                    color = Navy700,
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(text = message.text, color = OnDark, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            if (state.isLoading) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Box(
                            modifier = Modifier
                                .background(color = Navy700, shape = RoundedCornerShape(16.dp))
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Cyan400,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }

        // ── Structured AI summary bar ─────────────────────────────────────────
        state.latestStructured?.let { structured ->
            Divider(color = Navy700, thickness = 1.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Navy800)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                structured.riskLevel?.let {
                    Text("Risk: $it", style = MaterialTheme.typography.labelMedium, color = RiskAmber, fontWeight = FontWeight.Bold)
                }
                structured.recommendedAction?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = OnDarkMuted, modifier = Modifier.weight(1f))
                }
            }
        }

        // ── Input bar ─────────────────────────────────────────────────────────
        Divider(color = Navy700, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Navy800)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about flood risk…", color = OnDarkMuted) },
                singleLine = true,
                colors = floodTextFieldColors(),
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(
                onClick = {
                    if (prompt.isNotBlank()) {
                        viewModel.sendMessage(prompt)
                        prompt = ""
                    }
                },
                enabled = !state.isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .background(color = if (!state.isLoading) Cyan400 else Navy700, shape = RoundedCornerShape(24.dp))
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = if (!state.isLoading) Navy800 else OnDarkMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        state.error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
    }
}
