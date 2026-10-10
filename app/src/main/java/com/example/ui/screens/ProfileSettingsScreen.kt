package com.example.ui.screens

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.RastaRepository
import com.example.service.RastaVoiceForegroundService
import com.example.ui.theme.RastaCyan
import com.example.ui.theme.RastaError
import com.example.ui.theme.RastaSuccess
import com.example.voice.RastaVoiceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    repository: RastaRepository,
    voiceManager: RastaVoiceManager,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by repository.currentUser.collectAsState()
    val userLanguage by repository.userLanguage.collectAsState()
    val speechRate by repository.speechRate.collectAsState()
    val speechPitch by repository.speechPitch.collectAsState()
    val serverUrl by repository.serverUrl.collectAsState()

    var editServerUrl by remember(serverUrl) { mutableStateOf(serverUrl) }
    var foregroundServiceActive by remember { mutableStateOf(false) }

    val languages = listOf(
        "Roman Urdu",
        "Urdu",
        "English",
        "Hindi",
        "Punjabi",
        "Arabic",
        "Bengali",
        "Spanish",
        "French",
        "German",
        "Chinese"
    )

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & Preferences") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = RastaCyan,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.fullName ?: "Shoaib",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "@${currentUser?.username ?: "shoaib"} • ${currentUser?.role?.name ?: "USER"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Status: ${currentUser?.status?.name ?: "APPROVED"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RastaSuccess
                        )
                    }
                }
            }

            // Language & Multilingual Settings
            Text(
                text = "MULTILINGUAL & LOCALIZATION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RastaCyan
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preferred Language",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Rasta AI automatically responds and speaks in your chosen language. Urdu and Arabic activate right-to-left (RTL) mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = userLanguage,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            languages.forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang) },
                                    onClick = {
                                        repository.updateLanguage(lang)
                                        voiceManager.configureSettings(lang, speechRate, speechPitch)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Voice & Speech Synthesis Controls
            Text(
                text = "VOICE ENGINE & SPEECH SYNTHESIS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RastaCyan
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Speech Rate (${"%.1f".format(speechRate)}x)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Slider(
                        value = speechRate,
                        onValueChange = {
                            repository.updateSpeechRate(it)
                            voiceManager.configureSettings(userLanguage, it, speechPitch)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Speech Pitch (${"%.1f".format(speechPitch)}x)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Slider(
                        value = speechPitch,
                        onValueChange = {
                            repository.updateSpeechPitch(it)
                            voiceManager.configureSettings(userLanguage, speechRate, it)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 5
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val testPhrase = when (userLanguage) {
                                "Urdu" -> "جی شعیب! راستہ اے آئی آپ کی خدمت کے لیے تیار ہے۔"
                                "English" -> "Hello Shoaib! Rasta AI voice synthesis is ready."
                                else -> "Ji Shoaib, Rasta AI hazir hai. Main aap ke liye tayyar hoon."
                            }
                            voiceManager.speak(testPhrase)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Test Voice Speech Output")
                    }
                }
            }

            // Background Foreground Service
            Text(
                text = "BACKGROUND ASSISTANT SERVICE",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RastaCyan
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Background Voice Service",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = "Runs an authorized foreground service notification for continuous active listening.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = foregroundServiceActive,
                        onCheckedChange = { active ->
                            foregroundServiceActive = active
                            val intent = Intent(context, RastaVoiceForegroundService::class.java).apply {
                                action = if (active) RastaVoiceForegroundService.ACTION_START else RastaVoiceForegroundService.ACTION_STOP
                            }
                            if (active) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(intent)
                                } else {
                                    context.startService(intent)
                                }
                            } else {
                                context.stopService(intent)
                            }
                        }
                    )
                }
            }

            // Backend Server Configuration
            Text(
                text = "BACKEND SERVER URL",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RastaCyan
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "PHP REST API Endpoint",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "Connect to your custom hosted PHP backend with JSON storage, or use embedded storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = editServerUrl,
                        onValueChange = { editServerUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { repository.updateServerUrl(editServerUrl.trim()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Server URL")
                    }
                }
            }

            // Sign out button
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onLogout,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("profile_logout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = RastaError),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Sign Out of Rasta AI")
            }
        }
    }
}
