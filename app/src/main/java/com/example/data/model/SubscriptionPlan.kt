package com.example.data.model

data class SubscriptionPlan(
    val id: String,
    val name: String, // Free, Basic, Pro, Premium, Enterprise
    val priceMonthly: Double,
    val dailyAiQuota: Int,
    val monthlyAiQuota: Int,
    val voiceMinutesPerDay: Int,
    val allowsVoiceAssistant: Boolean = true,
    val allowsDeviceAutomation: Boolean = true,
    val allowsTranslation: Boolean = true,
    val allowsAppLaunching: Boolean = true,
    val allowsSmsDrafting: Boolean = true,
    val allowsCallDialing: Boolean = true,
    val allowsAlarmReminders: Boolean = true,
    val description: String
)

data class UserSubscription(
    val userId: String,
    val planId: String,
    val planName: String,
    val startDate: Long,
    val expireDate: Long,
    val usedToday: Int,
    val maxDailyQuota: Int,
    val status: String = "ACTIVE" // ACTIVE, EXPIRED, SUSPENDED, REVOKED
) {
    val isExpired: Boolean get() = System.currentTimeMillis() > expireDate
    val remainingDailyQuota: Int get() = (maxDailyQuota - usedToday).coerceAtLeast(0)
}

object DefaultPlans {
    val FREE = SubscriptionPlan(
        id = "plan_free",
        name = "Free",
        priceMonthly = 0.0,
        dailyAiQuota = 25,
        monthlyAiQuota = 500,
        voiceMinutesPerDay = 15,
        allowsVoiceAssistant = true,
        allowsDeviceAutomation = true,
        allowsTranslation = true,
        allowsAppLaunching = true,
        allowsSmsDrafting = false,
        allowsCallDialing = false,
        allowsAlarmReminders = true,
        description = "Standard access with 25 AI queries per day and essential device utilities."
    )

    val BASIC = SubscriptionPlan(
        id = "plan_basic",
        name = "Basic",
        priceMonthly = 4.99,
        dailyAiQuota = 100,
        monthlyAiQuota = 2500,
        voiceMinutesPerDay = 60,
        allowsVoiceAssistant = true,
        allowsDeviceAutomation = true,
        allowsTranslation = true,
        allowsAppLaunching = true,
        allowsSmsDrafting = true,
        allowsCallDialing = true,
        allowsAlarmReminders = true,
        description = "Enhanced speed, 100 AI queries/day, contacts & SMS drafting."
    )

    val PRO = SubscriptionPlan(
        id = "plan_pro",
        name = "Pro",
        priceMonthly = 14.99,
        dailyAiQuota = 500,
        monthlyAiQuota = 12000,
        voiceMinutesPerDay = 240,
        allowsVoiceAssistant = true,
        allowsDeviceAutomation = true,
        allowsTranslation = true,
        allowsAppLaunching = true,
        allowsSmsDrafting = true,
        allowsCallDialing = true,
        allowsAlarmReminders = true,
        description = "High-priority Gemini processing, unlimited voice interaction, full automation."
    )

    val PREMIUM = SubscriptionPlan(
        id = "plan_premium",
        name = "Premium",
        priceMonthly = 29.99,
        dailyAiQuota = 2000,
        monthlyAiQuota = 50000,
        voiceMinutesPerDay = 720,
        allowsVoiceAssistant = true,
        allowsDeviceAutomation = true,
        allowsTranslation = true,
        allowsAppLaunching = true,
        allowsSmsDrafting = true,
        allowsCallDialing = true,
        allowsAlarmReminders = true,
        description = "Ultra high capacity, real-time background listening, priority server routing."
    )

    val ENTERPRISE = SubscriptionPlan(
        id = "plan_enterprise",
        name = "Enterprise",
        priceMonthly = 99.99,
        dailyAiQuota = 10000,
        monthlyAiQuota = 250000,
        voiceMinutesPerDay = 2400,
        allowsVoiceAssistant = true,
        allowsDeviceAutomation = true,
        allowsTranslation = true,
        allowsAppLaunching = true,
        allowsSmsDrafting = true,
        allowsCallDialing = true,
        allowsAlarmReminders = true,
        description = "Custom dedicated quota, continuous multi-agent support, and SLA."
    )

    val ALL = listOf(FREE, BASIC, PRO, PREMIUM, ENTERPRISE)
}
