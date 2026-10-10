package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.RastaRepository
import com.example.ui.theme.RastaCyan
import com.example.ui.theme.RastaElectricBlue
import com.example.voice.RastaVoiceManager
import com.example.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantScreen(
    repository: RastaRepository,
    voiceManager: RastaVoiceManager,
    onNavigateBack: () -> Unit
) {
    val voiceState by voiceManager.voiceState.collectAsState()
    val lastSpoken by voiceManager.lastSpokenText.collectAsState()
    val rmsLevel by voiceManager.rmsLevel.collectAsState()
    val userLanguage by repository.userLanguage.collectAsState()

    // Pulsing animation for active voice orb
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rasta AI Voice Portal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (voiceState == VoiceState.SPEAKING) {
                        TextButton(onClick = { voiceManager.stopSpeaking() }) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop Voice", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status & Instructions
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    color = RastaCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(RastaCyan, RastaElectricBlue)))
                ) {
                    Text(
                        text = "Language: $userLanguage • Wake Word: \"Hey Rasta\"",
                        style = MaterialTheme.typography.labelMedium,
                        color = RastaCyan,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (voiceState) {
                        VoiceState.LISTENING -> "Listening to Shoaib..."
                        VoiceState.PROCESSING -> "Analyzing Command..."
                        VoiceState.SPEAKING -> "Rasta AI is Speaking..."
                        VoiceState.ERROR -> "Microphone Error / Permission Required"
                        VoiceState.IDLE -> "Tap Microphone to Speak"
                    },
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Try saying: \"Flashlight on karo\", \"WhatsApp kholo\", or ask any question.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Central Glowing Holographic Audio Sphere
            Box(
                modifier = Modifier
                    .size(240.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow ring
                Box(
                    modifier = Modifier
                        .size(230.dp)
                        .scale(if (voiceState == VoiceState.LISTENING) (1f + rmsLevel * 0.4f) else pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    RastaCyan.copy(alpha = if (voiceState == VoiceState.LISTENING) 0.35f else 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Middle cyber orb
                Box(
                    modifier = Modifier
                        .size(170.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    RastaElectricBlue.copy(alpha = 0.6f),
                                    RastaCyan.copy(alpha = 0.8f)
                                )
                            )
                        )
                        .border(3.dp, RastaCyan, CircleShape)
                        .clickable {
                            if (voiceState == VoiceState.LISTENING) {
                                voiceManager.stopListening()
                            } else {
                                voiceManager.startListening()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = "Microphone",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // Real-time Spoken Text & Action Transcript Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "TRANSCRIPT & ASSISTANT SPEECH",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = RastaCyan
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (lastSpoken.isNotBlank()) lastSpoken else "Ji Shoaib, Rasta AI hazir hai. Bataiye, main aap ke liye kya karun?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Bottom Action Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        if (voiceState == VoiceState.LISTENING) {
                            voiceManager.stopListening()
                        } else {
                            voiceManager.startListening()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("voice_listen_toggle_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.LISTENING) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (voiceState == VoiceState.LISTENING) "Stop Listening" else "Start Listening")
                }

                if (voiceState == VoiceState.SPEAKING) {
                    OutlinedButton(
                        onClick = { voiceManager.stopSpeaking() },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Interrupt Speech")
                    }
                }
            }
        }
    }
}
