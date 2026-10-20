package com.zoya.voice.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zoya.voice.data.SettingsRepository
import com.zoya.voice.domain.model.SessionStatus
import com.zoya.voice.ui.theme.*

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val settingsRepo = remember { SettingsRepository(context) }
    val userName by settingsRepo.userName.collectAsState(initial = "Babe")
    val geminiKey by settingsRepo.geminiKey.collectAsState(initial = "")

    var status by remember { mutableStateOf(SessionStatus.IDLE) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0A12), BgDark, Color(0xFF0A0A14))
                )
            )
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Zoya",
                    style = MaterialTheme.typography.headlineMedium,
                    color = NeonPink,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (status) {
                        SessionStatus.LISTENING -> "Listening…"
                        SessionStatus.SPEAKING -> "Speaking…"
                        SessionStatus.CONNECTING -> "Connecting…"
                        SessionStatus.ERROR -> errorMessage ?: "Something went wrong"
                        else -> "Tap the orb to talk"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(NeonPink, NeonRed))),
                contentAlignment = Alignment.Center
            ) {
                Text("Z", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Glowing Orb
        GlowingOrb(
            status = status,
            onClick = {
                if (geminiKey.isBlank()) {
                    errorMessage = "Add Gemini API key in Settings first"
                    status = SessionStatus.ERROR
                    return@GlowingOrb
                }
                status = when (status) {
                    SessionStatus.IDLE, SessionStatus.ERROR -> SessionStatus.CONNECTING
                    else -> SessionStatus.IDLE
                }
                errorMessage = null
                // Real Gemini Live connection will come in next steps
            }
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Hello, $userName",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary
        )
        Text(
            text = "How can I assist you today?",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        WaveformPlaceholder(active = status == SessionStatus.LISTENING || status == SessionStatus.SPEAKING)

        Spacer(modifier = Modifier.height(32.dp))

        FloatingActionButton(
            onClick = {
                if (geminiKey.isBlank()) {
                    errorMessage = "Add Gemini API key in Settings first"
                    status = SessionStatus.ERROR
                    return@FloatingActionButton
                }
                status = when (status) {
                    SessionStatus.IDLE, SessionStatus.ERROR -> SessionStatus.LISTENING
                    else -> SessionStatus.IDLE
                }
                errorMessage = null
            },
            containerColor = if (status == SessionStatus.LISTENING || status == SessionStatus.SPEAKING)
                NeonPink else BgElevated,
            contentColor = Color.White,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                imageVector = if (status == SessionStatus.LISTENING || status == SessionStatus.SPEAKING)
                    Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = "Mic",
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun GlowingOrb(status: SessionStatus, onClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (status == SessionStatus.LISTENING || status == SessionStatus.SPEAKING) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(160.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = if (status != SessionStatus.IDLE && status != SessionStatus.ERROR) {
                        listOf(Color(0xFFFF6B9D), NeonPink, NeonRed, Color(0xFF8B001F))
                    } else {
                        listOf(Color(0xFF3A3A4A), Color(0xFF1A1A24))
                    }
                )
            )
            .clickable { onClick() }
    ) {
        Text(
            text = "Z",
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun WaveformPlaceholder(active: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(40.dp)
    ) {
        repeat(20) { index ->
            val height = if (active) (12 + (index % 7) * 4).dp else 12.dp
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height)
                    .clip(CircleShape)
                    .background(if (active) NeonPink else TextMuted.copy(alpha = 0.4f))
            )
        }
    }
}