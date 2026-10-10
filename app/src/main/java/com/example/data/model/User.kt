package com.example.data.model

enum class UserStatus {
    PENDING,
    APPROVED,
    REJECTED,
    SUSPENDED,
    DELETED
}

enum class UserRole {
    ADMIN,
    USER
}

data class User(
    val id: String,
    val username: String,
    val fullName: String,
    val emailOrPhone: String,
    val role: UserRole = UserRole.USER,
    val status: UserStatus = UserStatus.PENDING,
    val subscriptionPlanId: String = "plan_free",
    val createdAt: Long = System.currentTimeMillis(),
    val mustChangePassword: Boolean = false,
    val approvedAt: Long? = null,
    val approvedBy: String? = null
)

data class AuthSession(
    val token: String,
    val user: User,
    val expiresAt: Long
)
