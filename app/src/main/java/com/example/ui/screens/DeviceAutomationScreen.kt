package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automation.DeviceActionManager
import com.example.ui.theme.RastaCyan
import com.example.ui.theme.RastaElectricBlue
import com.example.ui.theme.RastaSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceAutomationScreen(
    actionManager: DeviceActionManager,
    onNavigateBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    var flashlightOn by remember { mutableStateOf(false) }
    var batteryText by remember { mutableStateOf(actionManager.getBatteryStatus()) }

    var customAppQuery by remember { mutableStateOf("") }
    var dialNumber by remember { mutableStateOf("") }
    var smsNumber by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("Assalam-o-Alaikum Shoaib here.") }
    var searchQuery by remember { mutableStateOf("") }
    var alarmHour by remember { mutableStateOf("7") }
    var alarmMinute by remember { mutableStateOf("00") }

    var actionFeedback by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Automation & Actions") },
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
            // Action feedback banner if any
            if (actionFeedback != null) {
                Surface(
                    color = RastaElectricBlue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = RastaCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = actionFeedback ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { actionFeedback = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Quick Device Utilities (Flashlight & Battery)
            Text(
                text = "DEVICE HARDWARE UTILITIES",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RastaCyan
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Flashlight Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = if (flashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Flashlight",
                            tint = if (flashlightOn) RastaCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (flashlightOn) "Flashlight ON" else "Flashlight OFF",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = {
                                val res = actionManager.toggleFlashlight()
                                if (res.isSuccess) {
                                    flashlightOn = res.getOrThrow()
                                    actionFeedback = "Flashlight is now ${if (flashlightOn) "ON" else "OFF"}"
                                } else {
                                    actionFeedback = res.exceptionOrNull()?.message
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (flashlightOn) "Turn Off" else "Turn On", fontSize = 12.sp)
                        }
                    }
                }

                // Battery Status Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = "Battery",
                            tint = RastaSuccess,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Battery Status",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = batteryText,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FilledTonalButton(
                            onClick = {
                                batteryText = actionManager.getBatteryStatus()
                                actionFeedback = batteryText
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Refresh", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Quick App Launchers
            Text(
                text = "ONE-TAP APP CONTROL",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RastaCyan
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    val quickApps = listOf("WhatsApp", "YouTube", "Chrome", "Camera", "Maps", "Settings")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickApps.take(3).forEach { app ->
                            OutlinedButton(
                                onClick = {
                                    val r = actionManager.openApp(app)
                                    actionFeedback = r.getOrNull() ?: r.exceptionOrNull()?.message
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(app, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickApps.drop(3).forEach { app ->
                            OutlinedButton(
                                onClick = {
                                    if (app == "Settings") {
                                        actionManager.openSettings()
                                        actionFeedback = "Settings opened."
                                    } else {
                                        val r = actionManager.openApp(app)
                                        actionFeedback = r.getOrNull() ?: r.exceptionOrNull()?.message
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(app, fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customAppQuery,
                            onValueChange = { customAppQuery = it },
                            placeholder = { Text("Launch any app name (e.g. Spotify)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (customAppQuery.isNotBlank()) {
                                    val r = actionManager.openApp(customAppQuery)
                                    actionFeedback = r.getOrNull() ?: r.exceptionOrNull()?.message
                                }
                            }
                        ) {
                            Text("Launch")
                        }
                    }
                }
            }

            // Calls & SMS Messaging
            Text(
                text = "COMMUNICATION AUTOMATION",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RastaCyan
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Phone Call Dialer",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = dialNumber,
                            onValueChange = { dialNumber = it },
                            placeholder = { Text("Enter number to call") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (dialNumber.isNotBlank()) {
                                    actionManager.prepareDialCall(dialNumber)
                                    actionFeedback = "Dialer opened for $dialNumber"
                                }
                            }
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Dial")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "SMS Drafting with Confirmation",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = smsNumber,
                        onValueChange = { smsNumber = it },
                        label = { Text("Recipient Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = smsMessage,
                        onValueChange = { smsMessage = it },
                        label = { Text("SMS Message Body") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (smsNumber.isNotBlank()) {
                                actionManager.prepareSms(smsNumber, smsMessage)
                                actionFeedback = "SMS prepared for $smsNumber"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Review & Open SMS Composer")
                    }
                }
            }

            // Alarms & Google Web Search
            Text(
                text = "ALARMS & SEARCH",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RastaCyan
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Set Alarm / Reminder",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = alarmHour,
                            onValueChange = { alarmHour = it },
                            label = { Text("Hour (0-23)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = alarmMinute,
                            onValueChange = { alarmMinute = it },
                            label = { Text("Minute") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                val h = alarmHour.toIntOrNull() ?: 7
                                val m = alarmMinute.toIntOrNull() ?: 0
                                actionManager.setAlarm(message = "Rasta AI Reminder", hour = h, minutes = m)
                                actionFeedback = "Alarm configured for $h:$m"
                            }
                        ) {
                            Text("Set")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Web Search",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search Google via Rasta AI...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (searchQuery.isNotBlank()) {
                                    actionManager.performWebSearch(searchQuery)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
