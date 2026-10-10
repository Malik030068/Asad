package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.data.model.User
import com.example.data.model.UserStatus
import com.example.data.repository.RastaRepository
import com.example.ui.theme.RastaCyan
import com.example.ui.theme.RastaError
import com.example.ui.theme.RastaSuccess
import com.example.ui.theme.RastaWarning
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    repository: RastaRepository,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val usersList by repository.usersList.collectAsState()
    val auditLogs by repository.auditLogs.collectAsState()
    val aiConfig by repository.aiConfig.collectAsState()
    val plans by repository.plans.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Password change dialog for initial administrator Malik
    var showPasswordChangeDialog by remember {
        mutableStateOf(currentUser?.mustChangePassword == true)
    }
    var newPasswordInput by remember { mutableStateOf("") }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }

    // AI Provider form state
    var editApiKey by remember(aiConfig) { mutableStateOf(aiConfig.apiKey) }
    var editModel by remember(aiConfig) { mutableStateOf(aiConfig.model) }
    var testResultMsg by remember { mutableStateOf<String?>(null) }
    var isTestingAi by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Delete confirmation dialog
    var userToDelete by remember { mutableStateOf<User?>(null) }

    // Initial password change dialog
    if (showPasswordChangeDialog) {
        AlertDialog(
            onDismissRequest = { /* Must change initial password */ },
            title = { Text("Security Notice: Change Initial Password") },
            text = {
                Column {
                    Text(
                        "Administrator Malik, your current initial password is 'asad'. For system security, please set a new personal password now.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPasswordInput,
                        onValueChange = { newPasswordInput = it },
                        label = { Text("New Secure Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordChangeError != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = passwordChangeError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val res = repository.changePassword("Malik", newPasswordInput)
                        if (res.isSuccess) {
                            showPasswordChangeDialog = false
                        } else {
                            passwordChangeError = res.exceptionOrNull()?.message
                        }
                    }
                ) {
                    Text("Save Password")
                }
            }
        )
    }

    // Delete user confirmation dialog
    if (userToDelete != null) {
        val target = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete User: ${target.username}?") },
            text = { Text("Are you sure you want to permanently delete user account '${target.fullName}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        currentUser?.let { admin ->
                            repository.deleteUser(admin.username, target.id)
                        }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Control Portal") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out"
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
        ) {
            // Stats Overview Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val pendingCount = usersList.count { it.status == UserStatus.PENDING }
                val approvedCount = usersList.count { it.status == UserStatus.APPROVED }
                val totalCount = usersList.size

                StatMiniCard(
                    title = "Total Users",
                    value = "$totalCount",
                    color = RastaCyan,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Pending",
                    value = "$pendingCount",
                    color = if (pendingCount > 0) RastaWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                StatMiniCard(
                    title = "Approved",
                    value = "$approvedCount",
                    color = RastaSuccess,
                    modifier = Modifier.weight(1f)
                )
            }

            // Tab Navigation
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Users (${usersList.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("AI Provider") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Audit Logs") }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Users Management Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search by username or name...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val filteredUsers = usersList.filter {
                            it.username.contains(searchQuery, ignoreCase = true) ||
                                    it.fullName.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredUsers) { user ->
                                UserAdminCard(
                                    user = user,
                                    currentAdminName = currentUser?.username ?: "Malik",
                                    onApprove = { repository.approveUser(currentUser?.username ?: "Malik", user.id) },
                                    onReject = { repository.rejectUser(currentUser?.username ?: "Malik", user.id) },
                                    onSuspend = { repository.suspendUser(currentUser?.username ?: "Malik", user.id) },
                                    onReactivate = { repository.reactivateUser(currentUser?.username ?: "Malik", user.id) },
                                    onDelete = { userToDelete = user },
                                    onPlanChange = { planId ->
                                        repository.assignPlan(currentUser?.username ?: "Malik", user.id, planId)
                                    }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // AI Provider Configuration Tab
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Text(
                                    text = "Gemini AI Provider Configuration",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Configure the LLM engine for Rasta AI assistant. API keys are securely persisted locally on device and forwarded via backend proxy.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = editModel,
                                    onValueChange = { editModel = it },
                                    label = { Text("Model Identifier") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = editApiKey,
                                    onValueChange = { editApiKey = it },
                                    label = { Text("Gemini API Key") },
                                    placeholder = { Text("Enter Gemini API key") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_api_key_input"),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            repository.updateAiConfig(editApiKey.trim(), editModel.trim())
                                            testResultMsg = "AI Provider configuration saved successfully!"
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Save Settings")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            isTestingAi = true
                                            testResultMsg = null
                                            coroutineScope.launch {
                                                val res = com.example.data.repository.AiService().generateResponse(
                                                    prompt = "Test ping",
                                                    apiKeyOverride = editApiKey.trim()
                                                )
                                                isTestingAi = false
                                                if (res.isSuccess) {
                                                    testResultMsg = "Connection Successful! Model responded."
                                                } else {
                                                    testResultMsg = "Connection Error: ${res.exceptionOrNull()?.message}"
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isTestingAi) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                                        } else {
                                            Text("Test Connection")
                                        }
                                    }
                                }

                                if (testResultMsg != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        color = if (testResultMsg?.startsWith("Connection Error") == true)
                                            MaterialTheme.colorScheme.errorContainer
                                        else RastaCyan.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = testResultMsg ?: "",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Audit Logs Tab
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(auditLogs) { log ->
                            AuditLogItem(log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatMiniCard(
    title: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
    }
}

@Composable
fun UserAdminCard(
    user: User,
    currentAdminName: String,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onSuspend: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
    onPlanChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = user.fullName,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "@${user.username} • ${user.emailOrPhone}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = when (user.status) {
                        UserStatus.APPROVED -> RastaSuccess.copy(alpha = 0.2f)
                        UserStatus.PENDING -> RastaWarning.copy(alpha = 0.2f)
                        UserStatus.SUSPENDED -> RastaError.copy(alpha = 0.2f)
                        UserStatus.REJECTED -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        UserStatus.DELETED -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = user.status.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (user.status) {
                            UserStatus.APPROVED -> RastaSuccess
                            UserStatus.PENDING -> RastaWarning
                            UserStatus.SUSPENDED -> RastaError
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons based on status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (user.status == UserStatus.PENDING) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_approve_${user.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = RastaSuccess)
                    ) {
                        Text("Approve", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reject", fontSize = 12.sp)
                    }
                } else if (user.status == UserStatus.APPROVED) {
                    OutlinedButton(
                        onClick = onSuspend,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Suspend", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RastaError)
                    ) {
                        Text("Delete", fontSize = 12.sp)
                    }
                } else if (user.status == UserStatus.SUSPENDED) {
                    Button(
                        onClick = onReactivate,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reactivate", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RastaError)
                    ) {
                        Text("Delete", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AuditLogItem(log: AuditLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = log.action,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = RastaCyan
                )
                val dateStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${log.actor}: ${log.details}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
