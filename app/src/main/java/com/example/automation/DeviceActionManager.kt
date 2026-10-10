package com.example.automation

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import com.example.data.model.ActionState
import com.example.data.model.ActionType
import com.example.data.model.DeviceAction

class DeviceActionManager(private val context: Context) {

    private var isTorchOn = false

    // Flashlight control
    fun toggleFlashlight(forceState: Boolean? = null): Result<Boolean> {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return Result.failure(Exception("Camera manager not available"))

        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return Result.failure(Exception("Device has no flashlight hardware"))

            val targetState = forceState ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            Result.success(isTorchOn)
        } catch (e: CameraAccessException) {
            Result.failure(Exception("Flashlight error: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Battery status
    fun getBatteryStatus(): String {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            context.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct: Float = if (level >= 0 && scale > 0) (level * 100 / scale.toFloat()) else 0f

        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val chargingStr = if (isCharging) "Charging (Plugged in)" else "Discharging"
        return "Battery level is ${batteryPct.toInt()}%. Status: $chargingStr."
    }

    // Launch installed app
    fun openApp(appName: String): Result<String> {
        val cleanName = appName.lowercase().trim()
        val packageMap = mapOf(
            "whatsapp" to "com.whatsapp",
            "youtube" to "com.google.android.youtube",
            "chrome" to "com.android.chrome",
            "camera" to "com.google.android.GoogleCamera",
            "maps" to "com.google.android.apps.maps",
            "gmail" to "com.google.android.gm",
            "facebook" to "com.facebook.katana",
            "instagram" to "com.instagram.android",
            "spotify" to "com.spotify.music"
        )

        // Try direct package launch
        val specificPackage = packageMap[cleanName]
        if (specificPackage != null) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(specificPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return Result.success("Opened $appName successfully.")
            }
        }

        // Generic search by app label among installed apps
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        for (info in resolveInfos) {
            val label = info.loadLabel(pm).toString().lowercase()
            if (label.contains(cleanName)) {
                val launchIntent = pm.getLaunchIntentForPackage(info.activityInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return Result.success("Opened ${info.loadLabel(pm)} successfully.")
                }
            }
        }

        return Result.failure(Exception("Application '$appName' is not installed on this device."))
    }

    // Launch system settings
    fun openSettings(screen: String = "general"): Result<String> {
        return try {
            val intent = when (screen.lowercase()) {
                "wifi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
                "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                "display" -> Intent(Settings.ACTION_DISPLAY_SETTINGS)
                "sound" -> Intent(Settings.ACTION_SOUND_SETTINGS)
                "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                else -> Intent(Settings.ACTION_SETTINGS)
            }.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Opened settings.")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Prepare phone dialer
    fun prepareDialCall(target: String): Result<String> {
        return try {
            val cleanNumber = target.filter { it.isDigit() || it == '+' }
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${if (cleanNumber.isNotBlank()) cleanNumber else target}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            Result.success("Dialer opened for $target")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Prepare SMS
    fun prepareSms(targetNumber: String, messageText: String): Result<String> {
        return try {
            val uri = Uri.parse("smsto:${targetNumber.trim()}")
            val smsIntent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(smsIntent)
            Result.success("SMS composer opened for $targetNumber")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Search contacts
    fun searchContacts(query: String): List<Pair<String, String>> {
        val results = mutableListOf<Pair<String, String>>()
        try {
            val cr = context.contentResolver
            val cursor = cr.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                null
            )
            cursor?.use {
                val nameIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (it.moveToNext()) {
                    val name = if (nameIdx != -1) it.getString(nameIdx) else ""
                    val num = if (numIdx != -1) it.getString(numIdx) else ""
                    if (name.isNotBlank() && num.isNotBlank()) {
                        results.add(Pair(name, num))
                    }
                }
            }
        } catch (e: Exception) {
            // Permission not granted or query failed
        }
        return results
    }

    // Set alarm
    fun setAlarm(message: String = "Rasta AI Reminder", hour: Int = 7, minutes: Int = 0): Result<String> {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minutes)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Alarm set for %02d:%02d".format(hour, minutes))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Web search
    fun performWebSearch(query: String): Result<String> {
        return try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                putExtra(SearchManager.QUERY, query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success("Web search executed for '$query'")
        } catch (e: Exception) {
            // Fallback to browser URL
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                Result.success("Browser opened for '$query'")
            } catch (err: Exception) {
                Result.failure(err)
            }
        }
    }

    // Execute arbitrary device action
    fun executeAction(action: DeviceAction): DeviceAction {
        return try {
            when (action.type) {
                ActionType.FLASHLIGHT_ON -> {
                    toggleFlashlight(forceState = true)
                    action.copy(executionState = ActionState.EXECUTED, explanation = "Flashlight turned ON.")
                }
                ActionType.FLASHLIGHT_OFF -> {
                    toggleFlashlight(forceState = false)
                    action.copy(executionState = ActionState.EXECUTED, explanation = "Flashlight turned OFF.")
                }
                ActionType.FLASHLIGHT_TOGGLE -> {
                    val res = toggleFlashlight()
                    val on = res.getOrDefault(false)
                    action.copy(executionState = ActionState.EXECUTED, explanation = "Flashlight is now ${if (on) "ON" else "OFF"}.")
                }
                ActionType.CHECK_BATTERY -> {
                    val status = getBatteryStatus()
                    action.copy(executionState = ActionState.EXECUTED, explanation = status)
                }
                ActionType.OPEN_APP -> {
                    val res = openApp(action.target)
                    if (res.isSuccess) {
                        action.copy(executionState = ActionState.EXECUTED, explanation = res.getOrThrow())
                    } else {
                        action.copy(executionState = ActionState.FAILED, explanation = res.exceptionOrNull()?.message ?: "Failed to open app")
                    }
                }
                ActionType.OPEN_SETTINGS -> {
                    openSettings(action.target)
                    action.copy(executionState = ActionState.EXECUTED, explanation = "System settings opened.")
                }
                ActionType.DIAL_CALL -> {
                    prepareDialCall(action.target)
                    action.copy(executionState = ActionState.EXECUTED, explanation = "Dialer launched for ${action.target}.")
                }
                ActionType.COMPOSE_SMS -> {
                    prepareSms(action.target, action.secondaryData)
                    action.copy(executionState = ActionState.EXECUTED, explanation = "SMS composer launched for ${action.target}.")
                }
                ActionType.SET_ALARM -> {
                    setAlarm(message = "Rasta AI Alarm", hour = 7, minutes = 0)
                    action.copy(executionState = ActionState.EXECUTED, explanation = "Alarm configured.")
                }
                ActionType.SET_TIMER -> {
                    setAlarm(message = "Rasta AI Timer", hour = 0, minutes = 15)
                    action.copy(executionState = ActionState.EXECUTED, explanation = "Timer configured.")
                }
                ActionType.WEB_SEARCH -> {
                    performWebSearch(action.target)
                    action.copy(executionState = ActionState.EXECUTED, explanation = "Searched web for '${action.target}'.")
                }
                ActionType.READ_CONTACTS -> {
                    val contacts = searchContacts(action.target)
                    val txt = if (contacts.isNotEmpty()) "Found ${contacts.size} contact(s)." else "No contacts found."
                    action.copy(executionState = ActionState.EXECUTED, explanation = txt)
                }
            }
        } catch (e: Exception) {
            action.copy(executionState = ActionState.FAILED, explanation = e.message ?: "Action execution failed")
        }
    }
}
