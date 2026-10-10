package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.ui.navigation.AppScreen
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.voice.RastaVoiceManager

class MainActivity : ComponentActivity() {

    private lateinit var voiceManager: RastaVoiceManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as RastaApplication
        val repository = app.repository
        val actionManager = app.actionManager
        val aiService = app.aiService

        voiceManager = RastaVoiceManager(
            context = this,
            onSpeechRecognized = { text ->
                // Voice input captured
            },
            onWakeWordDetected = {
                val lang = repository.userLanguage.value
                val wakeReply = when (lang) {
                    "Urdu" -> "جی شعیب! راستہ اے آئی حاضر ہے۔ بتائیے، میں آپ کے لیے کیا کروں؟"
                    "English" -> "Yes Shoaib, Rasta AI is here! How can I help you?"
                    else -> "Ji Shoaib, Rasta AI hazir hai. Bataiye, main aap ke liye kya karun?"
                }
                voiceManager.speak(wakeReply)
            }
        )

        setContent {
            val isRtl by repository.isRtl.collectAsState()
            val currentUser by repository.currentUser.collectAsState()
            val userLanguage by repository.userLanguage.collectAsState()
            val speechRate by repository.speechRate.collectAsState()
            val speechPitch by repository.speechPitch.collectAsState()

            LaunchedEffect(userLanguage, speechRate, speechPitch) {
                voiceManager.configureSettings(userLanguage, speechRate, speechPitch)
            }

            // Permissions launcher
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* Permissions checked */ }

            LaunchedEffect(Unit) {
                val permissions = mutableListOf(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions.add(Manifest.permission.POST_NOTIFICATIONS)
                }
                permissionLauncher.launch(permissions.toTypedArray())
            }

            var currentScreen by remember {
                mutableStateOf(
                    when {
                        currentUser == null -> AppScreen.LOGIN
                        currentUser?.status == UserStatus.PENDING -> AppScreen.PENDING_APPROVAL
                        currentUser?.role == UserRole.ADMIN -> AppScreen.ADMIN_DASHBOARD
                        else -> AppScreen.CHAT
                    }
                )
            }

            // Keep screen synced if user state changes
            LaunchedEffect(currentUser) {
                if (currentUser == null) {
                    currentScreen = AppScreen.LOGIN
                } else if (currentUser?.status == UserStatus.PENDING && currentScreen != AppScreen.LOGIN) {
                    currentScreen = AppScreen.PENDING_APPROVAL
                }
            }

            MyApplicationTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                ) {
                    Scaffold(
                        bottomBar = {
                            val showBottomNav = currentUser != null &&
                                    currentUser?.status == UserStatus.APPROVED &&
                                    currentScreen != AppScreen.LOGIN &&
                                    currentScreen != AppScreen.REGISTER &&
                                    currentScreen != AppScreen.PENDING_APPROVAL

                            AnimatedVisibility(visible = showBottomNav) {
                                NavigationBar(
                                    windowInsets = WindowInsets.navigationBars
                                ) {
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.CHAT,
                                        onClick = { currentScreen = AppScreen.CHAT },
                                        icon = { Icon(Icons.Default.Chat, contentDescription = "Chat") },
                                        label = { Text("Assistant") }
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.VOICE,
                                        onClick = { currentScreen = AppScreen.VOICE },
                                        icon = { Icon(Icons.Default.Mic, contentDescription = "Voice") },
                                        label = { Text("Voice") }
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.AUTOMATION,
                                        onClick = { currentScreen = AppScreen.AUTOMATION },
                                        icon = { Icon(Icons.Default.SettingsRemote, contentDescription = "Automation") },
                                        label = { Text("Actions") }
                                    )
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.SUBSCRIPTION,
                                        onClick = { currentScreen = AppScreen.SUBSCRIPTION },
                                        icon = { Icon(Icons.Default.WorkspacePremium, contentDescription = "Subscriptions") },
                                        label = { Text("Plans") }
                                    )
                                    if (currentUser?.role == UserRole.ADMIN) {
                                        NavigationBarItem(
                                            selected = currentScreen == AppScreen.ADMIN_DASHBOARD,
                                            onClick = { currentScreen = AppScreen.ADMIN_DASHBOARD },
                                            icon = { Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin") },
                                            label = { Text("Admin") }
                                        )
                                    }
                                    NavigationBarItem(
                                        selected = currentScreen == AppScreen.SETTINGS,
                                        onClick = { currentScreen = AppScreen.SETTINGS },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                        label = { Text("Settings") }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                AppScreen.LOGIN -> {
                                    LoginScreen(
                                        repository = repository,
                                        onLoginSuccess = { isAdmin, isPending ->
                                            currentScreen = when {
                                                isPending -> AppScreen.PENDING_APPROVAL
                                                isAdmin -> AppScreen.ADMIN_DASHBOARD
                                                else -> AppScreen.CHAT
                                            }
                                        },
                                        onNavigateToRegister = { currentScreen = AppScreen.REGISTER }
                                    )
                                }
                                AppScreen.REGISTER -> {
                                    BackHandler { currentScreen = AppScreen.LOGIN }
                                    RegisterScreen(
                                        repository = repository,
                                        onRegisterSuccess = { currentScreen = AppScreen.PENDING_APPROVAL },
                                        onNavigateBack = { currentScreen = AppScreen.LOGIN }
                                    )
                                }
                                AppScreen.PENDING_APPROVAL -> {
                                    BackHandler {
                                        repository.logout()
                                        currentScreen = AppScreen.LOGIN
                                    }
                                    PendingApprovalScreen(
                                        repository = repository,
                                        onApproved = { currentScreen = AppScreen.CHAT },
                                        onLogout = {
                                            repository.logout()
                                            currentScreen = AppScreen.LOGIN
                                        }
                                    )
                                }
                                AppScreen.CHAT -> {
                                    ChatAssistantScreen(
                                        repository = repository,
                                        aiService = aiService,
                                        actionManager = actionManager,
                                        voiceManager = voiceManager,
                                        onOpenVoiceMode = { currentScreen = AppScreen.VOICE },
                                        onOpenAutomation = { currentScreen = AppScreen.AUTOMATION }
                                    )
                                }
                                AppScreen.VOICE -> {
                                    BackHandler { currentScreen = AppScreen.CHAT }
                                    VoiceAssistantScreen(
                                        repository = repository,
                                        voiceManager = voiceManager,
                                        onNavigateBack = { currentScreen = AppScreen.CHAT }
                                    )
                                }
                                AppScreen.AUTOMATION -> {
                                    BackHandler { currentScreen = AppScreen.CHAT }
                                    DeviceAutomationScreen(
                                        actionManager = actionManager,
                                        onNavigateBack = { currentScreen = AppScreen.CHAT }
                                    )
                                }
                                AppScreen.SUBSCRIPTION -> {
                                    BackHandler { currentScreen = AppScreen.CHAT }
                                    SubscriptionScreen(
                                        repository = repository,
                                        onNavigateBack = { currentScreen = AppScreen.CHAT }
                                    )
                                }
                                AppScreen.ADMIN_DASHBOARD -> {
                                    BackHandler { currentScreen = AppScreen.CHAT }
                                    AdminDashboardScreen(
                                        repository = repository,
                                        onNavigateBack = { currentScreen = AppScreen.CHAT },
                                        onLogout = {
                                            repository.logout()
                                            currentScreen = AppScreen.LOGIN
                                        }
                                    )
                                }
                                AppScreen.SETTINGS -> {
                                    BackHandler { currentScreen = AppScreen.CHAT }
                                    ProfileSettingsScreen(
                                        repository = repository,
                                        voiceManager = voiceManager,
                                        onNavigateBack = { currentScreen = AppScreen.CHAT },
                                        onLogout = {
                                            repository.logout()
                                            currentScreen = AppScreen.LOGIN
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceManager.destroy()
    }
}
