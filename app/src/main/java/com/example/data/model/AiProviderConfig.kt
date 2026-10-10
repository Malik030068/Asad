package com.example.data.model

data class AiProviderConfig(
    val provider: String = "Google Gemini",
    val model: String = "gemini-2.5-flash",
    val endpoint: String = "https://generativelanguage.googleapis.com/v1beta/models/",
    val apiKey: String = "",
    val timeoutSeconds: Int = 30,
    val isEnabled: Boolean = true
)

data class AuditLog(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val actor: String,
    val action: String,
    val details: String,
    val ipAddress: String = "127.0.0.1"
)
