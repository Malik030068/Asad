package com.example.data.model

enum class ActionType {
    FLASHLIGHT_TOGGLE,
    FLASHLIGHT_ON,
    FLASHLIGHT_OFF,
    CHECK_BATTERY,
    OPEN_APP,
    DIAL_CALL,
    COMPOSE_SMS,
    SET_ALARM,
    SET_TIMER,
    WEB_SEARCH,
    OPEN_SETTINGS,
    READ_CONTACTS
}

data class DeviceAction(
    val type: ActionType,
    val target: String = "", // e.g., app name, phone number, search query
    val secondaryData: String = "", // e.g., sms message text, alarm minutes
    val title: String,
    val explanation: String,
    val requiresConfirmation: Boolean = false,
    val executionState: ActionState = ActionState.PENDING
)

enum class ActionState {
    PENDING,
    CONFIRMED,
    EXECUTED,
    FAILED,
    CANCELLED
}
