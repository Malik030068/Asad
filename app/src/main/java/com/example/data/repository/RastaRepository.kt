package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AiProviderConfig
import com.example.data.model.AuditLog
import com.example.data.model.AuthSession
import com.example.data.model.DefaultPlans
import com.example.data.model.SubscriptionPlan
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.data.model.UserSubscription
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

class RastaRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("rasta_ai_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _usersList = MutableStateFlow<List<User>>(emptyList())
    val usersList: StateFlow<List<User>> = _usersList.asStateFlow()

    private val _plans = MutableStateFlow<List<SubscriptionPlan>>(DefaultPlans.ALL)
    val plans: StateFlow<List<SubscriptionPlan>> = _plans.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val _aiConfig = MutableStateFlow(AiProviderConfig())
    val aiConfig: StateFlow<AiProviderConfig> = _aiConfig.asStateFlow()

    private val _serverUrl = MutableStateFlow(
        prefs.getString("backend_url", "https://api.rasta.ai/v1") ?: "https://api.rasta.ai/v1"
    )
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _userLanguage = MutableStateFlow(
        prefs.getString("user_language", "Roman Urdu") ?: "Roman Urdu"
    )
    val userLanguage: StateFlow<String> = _userLanguage.asStateFlow()

    private val _speechRate = MutableStateFlow(prefs.getFloat("speech_rate", 1.0f))
    val speechRate: StateFlow<Float> = _speechRate.asStateFlow()

    private val _speechPitch = MutableStateFlow(prefs.getFloat("speech_pitch", 1.0f))
    val speechPitch: StateFlow<Float> = _speechPitch.asStateFlow()

    private val _isRtl = MutableStateFlow(
        _userLanguage.value == "Urdu" || _userLanguage.value == "Arabic"
    )
    val isRtl: StateFlow<Boolean> = _isRtl.asStateFlow()

    init {
        loadData()
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun loadData() {
        val usersJsonStr = prefs.getString("users_db", null)
        val passwordsMap = mutableMapOf<String, String>()

        if (usersJsonStr == null) {
            // First run bootstrap: Create administrator "Malik" with initial password "asad"
            val initialAdmin = User(
                id = "admin_malik",
                username = "Malik",
                fullName = "Malik Asad (Administrator)",
                emailOrPhone = "admin@rasta.ai",
                role = UserRole.ADMIN,
                status = UserStatus.APPROVED,
                subscriptionPlanId = "plan_enterprise",
                mustChangePassword = true,
                approvedAt = System.currentTimeMillis(),
                approvedBy = "SYSTEM"
            )

            // Default demo approved user for Shoaib
            val shoaibUser = User(
                id = "user_shoaib",
                username = "Shoaib",
                fullName = "Shoaib",
                emailOrPhone = "shoaib@rasta.ai",
                role = UserRole.USER,
                status = UserStatus.APPROVED,
                subscriptionPlanId = "plan_pro",
                mustChangePassword = false,
                approvedAt = System.currentTimeMillis(),
                approvedBy = "Malik"
            )

            val initialList = listOf(initialAdmin, shoaibUser)
            _usersList.value = initialList
            saveUsers(initialList)

            // Save initial password hashes
            savePasswordHash("Malik", hashPassword("asad"))
            savePasswordHash("Shoaib", hashPassword("shoaib123"))

            addAuditLog("SYSTEM", "INITIAL_SETUP", "Initial administrator Malik and user Shoaib created.")
        } else {
            // Parse stored users
            try {
                val array = JSONArray(usersJsonStr)
                val list = mutableListOf<User>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        User(
                            id = obj.getString("id"),
                            username = obj.getString("username"),
                            fullName = obj.getString("fullName"),
                            emailOrPhone = obj.getString("emailOrPhone"),
                            role = UserRole.valueOf(obj.getString("role")),
                            status = UserStatus.valueOf(obj.getString("status")),
                            subscriptionPlanId = obj.optString("subscriptionPlanId", "plan_free"),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            mustChangePassword = obj.optBoolean("mustChangePassword", false),
                            approvedAt = if (obj.has("approvedAt")) obj.getLong("approvedAt") else null,
                            approvedBy = obj.optString("approvedBy", null)
                        )
                    )
                }
                _usersList.value = list
            } catch (e: Exception) {
                _usersList.value = emptyList()
            }
        }

        // Load active session if any
        val loggedInUserId = prefs.getString("logged_in_user_id", null)
        if (loggedInUserId != null) {
            _currentUser.value = _usersList.value.find { it.id == loggedInUserId }
        }

        // Load AI Config
        val savedApiKey = prefs.getString("ai_api_key", "") ?: ""
        val savedModel = prefs.getString("ai_model", "gemini-2.5-flash") ?: "gemini-2.5-flash"
        _aiConfig.value = AiProviderConfig(apiKey = savedApiKey, model = savedModel)

        // Load Audit Logs
        loadAuditLogs()
    }

    private fun saveUsers(list: List<User>) {
        val array = JSONArray()
        for (u in list) {
            val obj = JSONObject().apply {
                put("id", u.id)
                put("username", u.username)
                put("fullName", u.fullName)
                put("emailOrPhone", u.emailOrPhone)
                put("role", u.role.name)
                put("status", u.status.name)
                put("subscriptionPlanId", u.subscriptionPlanId)
                put("createdAt", u.createdAt)
                put("mustChangePassword", u.mustChangePassword)
                if (u.approvedAt != null) put("approvedAt", u.approvedAt)
                if (u.approvedBy != null) put("approvedBy", u.approvedBy)
            }
            array.put(obj)
        }
        prefs.edit().putString("users_db", array.toString()).apply()
    }

    private fun savePasswordHash(username: String, hash: String) {
        prefs.edit().putString("pwd_hash_${username.lowercase()}", hash).apply()
    }

    private fun getPasswordHash(username: String): String? {
        return prefs.getString("pwd_hash_${username.lowercase()}", null)
    }

    // --- Authentication & Registration ---

    fun login(username: String, password: String): Result<User> {
        val cleanUser = username.trim()
        val user = _usersList.value.find { it.username.equals(cleanUser, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("Invalid username or password."))

        val expectedHash = getPasswordHash(cleanUser)
        val enteredHash = hashPassword(password)

        if (expectedHash == null || expectedHash != enteredHash) {
            addAuditLog(cleanUser, "FAILED_LOGIN", "Failed login attempt for user: $cleanUser")
            return Result.failure(IllegalArgumentException("Invalid username or password."))
        }

        if (user.status == UserStatus.SUSPENDED) {
            addAuditLog(cleanUser, "BLOCKED_LOGIN", "Suspended user attempted login.")
            return Result.failure(IllegalStateException("Account is suspended. Please contact administrator."))
        }

        if (user.status == UserStatus.REJECTED) {
            return Result.failure(IllegalStateException("Registration was rejected by administrator."))
        }

        if (user.status == UserStatus.DELETED) {
            return Result.failure(IllegalStateException("Account has been deleted."))
        }

        // Update active session
        _currentUser.value = user
        prefs.edit().putString("logged_in_user_id", user.id).apply()
        addAuditLog(user.username, "LOGIN_SUCCESS", "User logged in successfully (Role: ${user.role.name})")

        return Result.success(user)
    }

    fun register(
        fullName: String,
        username: String,
        emailOrPhone: String,
        password: String,
        termsAccepted: Boolean
    ): Result<User> {
        if (!termsAccepted) {
            return Result.failure(IllegalArgumentException("You must accept the terms and conditions."))
        }
        if (fullName.isBlank() || username.isBlank() || emailOrPhone.isBlank() || password.length < 4) {
            return Result.failure(IllegalArgumentException("Please fill all fields properly (password min 4 chars)."))
        }

        val exists = _usersList.value.any { it.username.equals(username.trim(), ignoreCase = true) }
        if (exists) {
            return Result.failure(IllegalArgumentException("Username already taken. Please choose another."))
        }

        val newUser = User(
            id = "user_" + UUID.randomUUID().toString().take(8),
            username = username.trim(),
            fullName = fullName.trim(),
            emailOrPhone = emailOrPhone.trim(),
            role = UserRole.USER,
            status = UserStatus.PENDING,
            subscriptionPlanId = "plan_free",
            createdAt = System.currentTimeMillis()
        )

        val updated = _usersList.value + newUser
        _usersList.value = updated
        saveUsers(updated)
        savePasswordHash(newUser.username, hashPassword(password))

        // Auto login session in PENDING state
        _currentUser.value = newUser
        prefs.edit().putString("logged_in_user_id", newUser.id).apply()

        addAuditLog(newUser.username, "USER_REGISTRATION", "New user registered with PENDING status.")
        return Result.success(newUser)
    }

    fun changePassword(username: String, newPassword: String): Result<Unit> {
        if (newPassword.length < 4) {
            return Result.failure(IllegalArgumentException("New password must be at least 4 characters."))
        }
        val target = _usersList.value.find { it.username.equals(username, ignoreCase = true) }
            ?: return Result.failure(IllegalArgumentException("User not found."))

        savePasswordHash(target.username, hashPassword(newPassword))

        // Remove mustChangePassword flag
        val updated = _usersList.value.map {
            if (it.id == target.id) it.copy(mustChangePassword = false) else it
        }
        _usersList.value = updated
        saveUsers(updated)

        if (_currentUser.value?.id == target.id) {
            _currentUser.value = target.copy(mustChangePassword = false)
        }

        addAuditLog(target.username, "PASSWORD_CHANGED", "Password updated successfully.")
        return Result.success(Unit)
    }

    fun refreshUserStatus(): User? {
        val current = _currentUser.value ?: return null
        val refreshed = _usersList.value.find { it.id == current.id }
        if (refreshed != null) {
            _currentUser.value = refreshed
        }
        return refreshed
    }

    fun logout() {
        val user = _currentUser.value
        if (user != null) {
            addAuditLog(user.username, "LOGOUT", "User logged out.")
        }
        _currentUser.value = null
        prefs.edit().remove("logged_in_user_id").apply()
    }

    // --- Administrator Operations ---

    fun approveUser(adminName: String, userId: String): Result<User> {
        val user = _usersList.value.find { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("User not found."))

        val updatedUser = user.copy(
            status = UserStatus.APPROVED,
            approvedAt = System.currentTimeMillis(),
            approvedBy = adminName
        )

        val updated = _usersList.value.map { if (it.id == userId) updatedUser else it }
        _usersList.value = updated
        saveUsers(updated)

        if (_currentUser.value?.id == userId) {
            _currentUser.value = updatedUser
        }

        addAuditLog(adminName, "USER_APPROVED", "User ${user.username} approved by $adminName")
        return Result.success(updatedUser)
    }

    fun rejectUser(adminName: String, userId: String): Result<User> {
        val user = _usersList.value.find { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("User not found."))

        val updatedUser = user.copy(status = UserStatus.REJECTED)
        val updated = _usersList.value.map { if (it.id == userId) updatedUser else it }
        _usersList.value = updated
        saveUsers(updated)

        addAuditLog(adminName, "USER_REJECTED", "User ${user.username} rejected by $adminName")
        return Result.success(updatedUser)
    }

    fun suspendUser(adminName: String, userId: String): Result<User> {
        val user = _usersList.value.find { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("User not found."))

        if (user.role == UserRole.ADMIN) {
            return Result.failure(IllegalArgumentException("Cannot suspend an administrator account."))
        }

        val updatedUser = user.copy(status = UserStatus.SUSPENDED)
        val updated = _usersList.value.map { if (it.id == userId) updatedUser else it }
        _usersList.value = updated
        saveUsers(updated)

        addAuditLog(adminName, "USER_SUSPENDED", "User ${user.username} suspended by $adminName")
        return Result.success(updatedUser)
    }

    fun reactivateUser(adminName: String, userId: String): Result<User> {
        val user = _usersList.value.find { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("User not found."))

        val updatedUser = user.copy(status = UserStatus.APPROVED)
        val updated = _usersList.value.map { if (it.id == userId) updatedUser else it }
        _usersList.value = updated
        saveUsers(updated)

        addAuditLog(adminName, "USER_REACTIVATED", "User ${user.username} reactivated by $adminName")
        return Result.success(updatedUser)
    }

    fun deleteUser(adminName: String, userId: String): Result<Unit> {
        val user = _usersList.value.find { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("User not found."))

        if (user.role == UserRole.ADMIN && user.username.equals("Malik", ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("Cannot delete primary administrator Malik."))
        }

        val updated = _usersList.value.filter { it.id != userId }
        _usersList.value = updated
        saveUsers(updated)

        addAuditLog(adminName, "USER_DELETED", "User ${user.username} deleted permanently.")
        return Result.success(Unit)
    }

    fun assignPlan(adminName: String, userId: String, planId: String): Result<User> {
        val user = _usersList.value.find { it.id == userId }
            ?: return Result.failure(IllegalArgumentException("User not found."))

        val plan = _plans.value.find { it.id == planId }
            ?: return Result.failure(IllegalArgumentException("Invalid plan ID."))

        val updatedUser = user.copy(subscriptionPlanId = plan.id)
        val updated = _usersList.value.map { if (it.id == userId) updatedUser else it }
        _usersList.value = updated
        saveUsers(updated)

        if (_currentUser.value?.id == userId) {
            _currentUser.value = updatedUser
        }

        addAuditLog(adminName, "PLAN_ASSIGNED", "Assigned ${plan.name} to ${user.username}")
        return Result.success(updatedUser)
    }

    // --- Subscriptions & Quota Enforcement ---

    fun getUserSubscription(userId: String): UserSubscription {
        val user = _usersList.value.find { it.id == userId }
        val plan = _plans.value.find { it.id == (user?.subscriptionPlanId ?: "plan_free") }
            ?: DefaultPlans.FREE

        val usedToday = prefs.getInt("quota_used_${userId}_${getTodayKey()}", 0)
        val expireDate = prefs.getLong("plan_expire_${userId}", System.currentTimeMillis() + 30L * 24 * 3600 * 1000)

        return UserSubscription(
            userId = userId,
            planId = plan.id,
            planName = plan.name,
            startDate = System.currentTimeMillis() - 86400000L,
            expireDate = expireDate,
            usedToday = usedToday,
            maxDailyQuota = plan.dailyAiQuota,
            status = "ACTIVE"
        )
    }

    fun checkAndConsumeAiQuota(userId: String): Boolean {
        val sub = getUserSubscription(userId)
        if (sub.isExpired || sub.remainingDailyQuota <= 0) {
            return false
        }
        val key = "quota_used_${userId}_${getTodayKey()}"
        prefs.edit().putInt(key, sub.usedToday + 1).apply()
        return true
    }

    private fun getTodayKey(): String {
        val cal = java.util.Calendar.getInstance()
        return "${cal.get(java.util.Calendar.YEAR)}_${cal.get(java.util.Calendar.DAY_OF_YEAR)}"
    }

    // --- AI Config & Server Settings ---

    fun updateAiConfig(apiKey: String, model: String) {
        _aiConfig.value = _aiConfig.value.copy(apiKey = apiKey, model = model)
        prefs.edit()
            .putString("ai_api_key", apiKey)
            .putString("ai_model", model)
            .apply()
        addAuditLog(_currentUser.value?.username ?: "ADMIN", "AI_CONFIG_UPDATED", "Updated model to $model and API key")
    }

    fun updateServerUrl(url: String) {
        _serverUrl.value = url
        prefs.edit().putString("backend_url", url).apply()
    }

    fun updateLanguage(lang: String) {
        _userLanguage.value = lang
        _isRtl.value = lang == "Urdu" || lang == "Arabic"
        prefs.edit().putString("user_language", lang).apply()
    }

    fun updateSpeechRate(rate: Float) {
        _speechRate.value = rate
        prefs.edit().putFloat("speech_rate", rate).apply()
    }

    fun updateSpeechPitch(pitch: Float) {
        _speechPitch.value = pitch
        prefs.edit().putFloat("speech_pitch", pitch).apply()
    }

    // --- Audit Logs ---

    fun addAuditLog(actor: String, action: String, details: String) {
        val log = AuditLog(
            id = "log_" + UUID.randomUUID().toString().take(6),
            actor = actor,
            action = action,
            details = details
        )
        val currentLogs = _auditLogs.value
        val updated = listOf(log) + currentLogs.take(99)
        _auditLogs.value = updated
        saveAuditLogs(updated)
    }

    private fun saveAuditLogs(list: List<AuditLog>) {
        val array = JSONArray()
        for (l in list) {
            val obj = JSONObject().apply {
                put("id", l.id)
                put("timestamp", l.timestamp)
                put("actor", l.actor)
                put("action", l.action)
                put("details", l.details)
                put("ipAddress", l.ipAddress)
            }
            array.put(obj)
        }
        prefs.edit().putString("audit_logs_db", array.toString()).apply()
    }

    private fun loadAuditLogs() {
        val raw = prefs.getString("audit_logs_db", null) ?: return
        try {
            val array = JSONArray(raw)
            val list = mutableListOf<AuditLog>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list.add(
                    AuditLog(
                        id = o.getString("id"),
                        timestamp = o.getLong("timestamp"),
                        actor = o.getString("actor"),
                        action = o.getString("action"),
                        details = o.getString("details"),
                        ipAddress = o.optString("ipAddress", "127.0.0.1")
                    )
                )
            }
            _auditLogs.value = list
        } catch (e: Exception) {
            _auditLogs.value = emptyList()
        }
    }
}
