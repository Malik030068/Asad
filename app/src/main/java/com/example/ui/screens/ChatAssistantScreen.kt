package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.automation.DeviceActionManager
import com.example.data.model.ActionState
import com.example.data.model.ChatMessage
import com.example.data.model.DeviceAction
import com.example.data.model.MessageSender
import com.example.data.repository.AiService
import com.example.data.repository.RastaRepository
import com.example.ui.theme.RastaCyan
import com.example.ui.theme.RastaDarkSurfaceVariant
import com.example.ui.theme.RastaElectricBlue
import com.example.ui.theme.RastaSuccess
import com.example.voice.RastaVoiceManager
import com.example.voice.VoiceState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatAssistantScreen(
    repository: RastaRepository,
    aiService: AiService,
    actionManager: DeviceActionManager,
    voiceManager: RastaVoiceManager,
    onOpenVoiceMode: () -> Unit,
    onOpenAutomation: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val aiConfig by repository.aiConfig.collectAsState()
    val userLanguage by repository.userLanguage.collectAsState()
    val voiceState by voiceManager.voiceState.collectAsState()

    val userName = currentUser?.username ?: "Shoaib"
    val sub = currentUser?.let { repository.getUserSubscription(it.id) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var inputText by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }

    // Initial greeting in Roman Urdu / Urdu / English as requested
    val initialGreeting = remember(userName, userLanguage) {
        when (userLanguage) {
            "Urdu" -> "السلام علیکم $userName! میں راستہ اے آئی ہوں۔ آپ کو کس چیز میں مدد چاہیے؟"
            "English" -> "Hello $userName! I am Rasta AI. How can I assist you today?"
            else -> "Assalam-o-Alaikum $userName! Main Rasta AI hoon. Aap ko kis cheez mein madad chahiye?"
        }
    }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                id = "init_0",
                text = initialGreeting,
                sender = MessageSender.ASSISTANT,
                language = userLanguage
            )
        )
    }

    fun sendMessage(text: String) {
        val query = text.trim()
        if (query.isBlank() || isThinking) return

        val userMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            text = query,
            sender = MessageSender.USER,
            language = userLanguage
        )
        messages.add(userMsg)
        inputText = ""

        // Check subscription quota
        val hasQuota = currentUser?.let { repository.checkAndConsumeAiQuota(it.id) } ?: true
        if (!hasQuota) {
            messages.add(
                ChatMessage(
                    id = "quota_err_${System.currentTimeMillis()}",
                    text = "Daily AI quota reached for your plan. Please upgrade to Pro or Enterprise in Subscriptions.",
                    sender = MessageSender.SYSTEM
                )
            )
            return
        }

        isThinking = true
        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)

            val conversationHistory = messages.takeLast(6).map {
                (if (it.sender == MessageSender.USER) "USER" else "MODEL") to it.text
            }

            val result = aiService.generateResponse(
                prompt = query,
                apiKeyOverride = aiConfig.apiKey,
                userLanguage = userLanguage,
                conversationHistory = conversationHistory
            )

            isThinking = false

            if (result.isSuccess) {
                val (replyText, action) = result.getOrThrow()
                val assistantMsg = ChatMessage(
                    id = "msg_asst_${System.currentTimeMillis()}",
                    text = replyText,
                    sender = MessageSender.ASSISTANT,
                    language = userLanguage,
                    suggestedAction = action
                )
                messages.add(assistantMsg)
                listState.animateScrollToItem(messages.size - 1)

                // Optional speech
                voiceManager.speak(replyText)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "AI error"
                messages.add(
                    ChatMessage(
                        id = "msg_err_${System.currentTimeMillis()}",
                        text = "Rasta AI: $errorMsg",
                        sender = MessageSender.SYSTEM
                    )
                )
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    val quickChips = listOf(
        "Torch jalao" to "Flashlight On",
        "Battery kitni hai?" to "Battery Check",
        "Open WhatsApp" to "WhatsApp",
        "Alarm lagao 7 baje ka" to "Set Alarm",
        "Open Settings" to "Settings",
        "Google pe search karo AI" to "Web Search"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Assistant Top Header Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(RastaCyan.copy(alpha = 0.2f))
                            .border(1.5.dp, RastaCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "Rasta AI",
                            tint = RastaCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Rasta AI",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = RastaSuccess.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = RastaSuccess,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Shoaib • ${sub?.planName ?: "Pro Plan"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Remaining Quota Badge
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = "Quota: ${sub?.remainingDailyQuota ?: 500}",
                                fontSize = 11.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Bolt,
                                contentDescription = null,
                                tint = RastaCyan,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(onClick = onOpenAutomation) {
                        Icon(
                            imageVector = Icons.Default.SettingsRemote,
                            contentDescription = "Device Control",
                            tint = RastaCyan
                        )
                    }
                }
            }
        }

        // Quick Suggestion Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(quickChips) { (prompt, label) ->
                SuggestionChip(
                    onClick = { sendMessage(prompt) },
                    label = { Text(label, fontSize = 12.sp) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = RastaCyan
                        )
                    }
                )
            }
        }

        // Active TTS speaking alert bar if playing
        if (voiceState == VoiceState.SPEAKING) {
            Surface(
                color = RastaCyan.copy(alpha = 0.15f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.VolumeUp,
                            contentDescription = null,
                            tint = RastaCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rasta AI is speaking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = RastaCyan
                        )
                    }
                    TextButton(onClick = { voiceManager.stopSpeaking() }) {
                        Text("Stop", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }

        // Chat Conversation List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(
                    message = msg,
                    onSpeak = { voiceManager.speak(msg.text) },
                    onExecuteAction = { action ->
                        val executed = actionManager.executeAction(action)
                        val idx = messages.indexOf(msg)
                        if (idx != -1) {
                            messages[idx] = msg.copy(suggestedAction = executed)
                        }
                    }
                )
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = RastaCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Rasta AI is thinking...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Chat Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Voice Orb Launcher Button
                IconButton(
                    onClick = onOpenVoiceMode,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(RastaCyan.copy(alpha = 0.2f))
                        .testTag("voice_mode_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Voice Mode",
                        tint = RastaCyan
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = if (userLanguage == "Urdu") "یہاں پیغام لکھیں..." else "Ask Rasta AI...",
                            fontSize = 14.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RastaCyan,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { sendMessage(inputText) },
                    enabled = inputText.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank()) RastaElectricBlue else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("send_message_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubble(
    message: ChatMessage,
    onSpeak: () -> Unit,
    onExecuteAction: (DeviceAction) -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val isSystem = message.sender == MessageSender.SYSTEM

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        if (isSystem) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(8.dp)
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) RastaElectricBlue else RastaDarkSurfaceVariant
                ),
                modifier = Modifier.widthIn(max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )

                    // Optional TTS Listen Button for Assistant Messages
                    if (!isUser) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(
                                onClick = onSpeak,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = "Listen",
                                    tint = RastaCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Suggested Device Action Card
                    if (message.suggestedAction != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val action = message.suggestedAction
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.SettingsSuggest,
                                        contentDescription = null,
                                        tint = RastaCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = action.title,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = action.explanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                when (action.executionState) {
                                    ActionState.PENDING -> {
                                        Button(
                                            onClick = { onExecuteAction(action) },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 4.dp)
                                        ) {
                                            Text("Execute Action", fontSize = 12.sp)
                                        }
                                    }
                                    ActionState.EXECUTED -> {
                                        Surface(
                                            color = RastaSuccess.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "✓ Action Executed",
                                                color = RastaSuccess,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    }
                                    ActionState.FAILED -> {
                                        Surface(
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "✗ Execution Failed",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
