package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ActionType
import com.example.data.model.DefaultPlans
import com.example.data.model.UserRole
import com.example.data.model.UserStatus
import com.example.data.repository.AiService
import com.example.data.repository.RastaRepository
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var repository: RastaRepository
    private val aiService = AiService()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext<Context>()
        repository = RastaRepository(context)
    }

    @Test
    fun testAppNameResource() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Rasta AI", appName)
    }

    @Test
    fun testInitialAdminLoginAndFirstRunPasswordRequirement() {
        // Administrator Malik login with initial password 'asad'
        val loginResult = repository.login("Malik", "asad")
        assertTrue("Malik should login successfully", loginResult.isSuccess)

        val adminUser = loginResult.getOrThrow()
        assertEquals(UserRole.ADMIN, adminUser.role)
        assertEquals(UserStatus.APPROVED, adminUser.status)
        assertTrue("Initial admin must be flagged to change password", adminUser.mustChangePassword)

        // Change password
        val changeResult = repository.changePassword("Malik", "newAdminPass123")
        assertTrue(changeResult.isSuccess)

        // Old password must fail
        val oldLogin = repository.login("Malik", "asad")
        assertFalse(oldLogin.isSuccess)

        // New password must succeed
        val newLogin = repository.login("Malik", "newAdminPass123")
        assertTrue(newLogin.isSuccess)
        assertFalse("mustChangePassword should now be cleared", newLogin.getOrThrow().mustChangePassword)
    }

    @Test
    fun testUserRegistrationAndPendingApprovalWorkflow() {
        val regResult = repository.register(
            fullName = "Test User",
            username = "testuser_99",
            emailOrPhone = "test@example.com",
            password = "password123",
            termsAccepted = true
        )
        assertTrue("User registration should succeed", regResult.isSuccess)
        val registeredUser = regResult.getOrThrow()
        assertEquals(UserStatus.PENDING, registeredUser.status)

        // Admin Malik approves user
        val approveResult = repository.approveUser("Malik", registeredUser.id)
        assertTrue(approveResult.isSuccess)
        assertEquals(UserStatus.APPROVED, approveResult.getOrThrow().status)
    }

    @Test
    fun testSubscriptionQuotasAndEntitlements() {
        val user = repository.login("Shoaib", "shoaib123").getOrThrow()
        val sub = repository.getUserSubscription(user.id)
        assertNotNull(sub)
        assertTrue("Initial daily quota should be > 0", sub.remainingDailyQuota > 0)

        // Consuming quota
        val consumed = repository.checkAndConsumeAiQuota(user.id)
        assertTrue(consumed)

        // Upgrading to Enterprise plan
        val upgradeResult = repository.assignPlan("Malik", user.id, DefaultPlans.ENTERPRISE.id)
        assertTrue(upgradeResult.isSuccess)
        val updatedSub = repository.getUserSubscription(user.id)
        assertEquals("Enterprise", updatedSub.planName)
    }

    @Test
    fun testDeviceActionIntentParsing() {
        val flashAction = aiService.analyzeLocalAction("Flashlight on karo")
        assertNotNull(flashAction)
        assertEquals(ActionType.FLASHLIGHT_ON, flashAction?.type)

        val battAction = aiService.analyzeLocalAction("Battery kitni hai?")
        assertNotNull(battAction)
        assertEquals(ActionType.CHECK_BATTERY, battAction?.type)

        val appAction = aiService.analyzeLocalAction("Open WhatsApp")
        assertNotNull(appAction)
        assertEquals(ActionType.OPEN_APP, appAction?.type)
        assertEquals("whatsapp", appAction?.target)

        val dialAction = aiService.analyzeLocalAction("Call 03001234567")
        assertNotNull(dialAction)
        assertEquals(ActionType.DIAL_CALL, dialAction?.type)
    }
}
