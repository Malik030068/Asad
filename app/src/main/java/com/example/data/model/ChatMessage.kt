package com.example.data.model

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: String,
    val text: String,
    val sender: MessageSender,
    val timestamp: Long = System.currentTimeMillis(),
    val language: String = "Roman Urdu",
    val detectedIntent: String? = null,
    val suggestedAction: DeviceAction? = null,
    val isSpoken: Boolean = false
)
