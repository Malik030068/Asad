package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DefaultPlans
import com.example.data.repository.RastaRepository
import com.example.ui.theme.RastaCyan
import com.example.ui.theme.RastaElectricBlue
import com.example.ui.theme.RastaSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    repository: RastaRepository,
    onNavigateBack: () -> Unit
) {
    val currentUser by repository.currentUser.collectAsState()
    val plans by repository.plans.collectAsState()

    val sub = currentUser?.let { repository.getUserSubscription(it.id) }
    val currentPlan = plans.find { it.id == (sub?.planId ?: "plan_free") } ?: DefaultPlans.FREE

    val scrollState = rememberScrollState()
    var upgradeMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriptions & Entitlements") },
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
            // Current Plan Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "CURRENT MEMBERSHIP",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = RastaCyan
                            )
                            Text(
                                text = currentPlan.name + " Plan",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            color = RastaSuccess.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                color = RastaSuccess,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quota Usage Progress
                    val totalQuota = currentPlan.dailyAiQuota
                    val remaining = sub?.remainingDailyQuota ?: totalQuota
                    val used = (totalQuota - remaining).coerceAtLeast(0)
                    val progress = if (totalQuota > 0) used.toFloat() / totalQuota.toFloat() else 0f

                    Text(
                        text = "Daily AI Request Quota: $remaining / $totalQuota Remaining",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = RastaElectricBlue,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "• Voice Session Limit: ${currentPlan.voiceMinutesPerDay} minutes/day\n" +
                                "• Mobile Automation: ${if (currentPlan.allowsDeviceAutomation) "Enabled" else "Restricted"}\n" +
                                "• SMS & Call Drafting: ${if (currentPlan.allowsSmsDrafting) "Enabled" else "Pro/Enterprise Only"}\n" +
                                "• High-Priority Routing: ${if (currentPlan.id != "plan_free") "Active" else "Standard"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (upgradeMessage != null) {
                Surface(
                    color = RastaCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = upgradeMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = RastaCyan,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Text(
                text = "AVAILABLE TIERS & PLANS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = RastaCyan
            )

            // List of Plans
            plans.forEach { plan ->
                val isSelected = plan.id == currentPlan.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = RastaCyan)
                                    Spacer(modifier = Modifier.width(6.dp))
                                } else {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = RastaElectricBlue)
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = plan.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Text(
                                text = if (plan.priceMonthly == 0.0) "Free" else "$${plan.priceMonthly}/mo",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = RastaCyan
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = plan.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Daily Quota: ${plan.dailyAiQuota} requests • Voice: ${plan.voiceMinutesPerDay} mins/day",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        if (!isSelected) {
                            OutlinedButton(
                                onClick = {
                                    currentUser?.let { user ->
                                        repository.assignPlan("Shoaib", user.id, plan.id)
                                        upgradeMessage = "Plan upgraded to ${plan.name} successfully!"
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("upgrade_plan_${plan.id}")
                            ) {
                                Text("Switch to ${plan.name}")
                            }
                        }
                    }
                }
            }
        }
    }
}
