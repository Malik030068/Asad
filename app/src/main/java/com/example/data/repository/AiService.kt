package com.example.data.repository

import com.example.BuildConfig
import com.example.data.model.ActionType
import com.example.data.model.DeviceAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Detect device action intents from natural language
    fun analyzeLocalAction(query: String): DeviceAction? {
        val q = query.lowercase().trim()

        // Flashlight intents
        if (q.contains("flashlight on") || q.contains("torch on") || q.contains("batti jalao") || q.contains("torch jalao")) {
            return DeviceAction(
                type = ActionType.FLASHLIGHT_ON,
                title = "Flashlight On",
                explanation = "Turning on device flashlight",
                requiresConfirmation = false
            )
        }
        if (q.contains("flashlight off") || q.contains("torch off") || q.contains("torch band") || q.contains("batti bujhao")) {
            return DeviceAction(
                type = ActionType.FLASHLIGHT_OFF,
                title = "Flashlight Off",
                explanation = "Turning off device flashlight",
                requiresConfirmation = false
            )
        }
        if (q.contains("torch") || q.contains("flashlight")) {
            return DeviceAction(
                type = ActionType.FLASHLIGHT_TOGGLE,
                title = "Toggle Flashlight",
                explanation = "Toggle device torch light",
                requiresConfirmation = false
            )
        }

        // Battery intent
        if (q.contains("battery") || q.contains("charging") || q.contains("charge kitna") || q.contains("battery percentage")) {
            return DeviceAction(
                type = ActionType.CHECK_BATTERY,
                title = "Battery Status",
                explanation = "Checking device battery percentage and charging state",
                requiresConfirmation = false
            )
        }

        // Settings intent
        if (q.contains("settings kholo") || q.contains("open settings") || q.contains("settings open")) {
            return DeviceAction(
                type = ActionType.OPEN_SETTINGS,
                title = "Android Settings",
                explanation = "Opening system settings",
                requiresConfirmation = false
            )
        }

        // App Open intents
        val apps = listOf("whatsapp", "youtube", "camera", "chrome", "maps", "gallery", "calculator", "clock", "spotify", "gmail", "facebook", "instagram")
        for (app in apps) {
            if (q.contains("open $app") || q.contains("$app kholo") || q.contains("launch $app") || q.contains("chalao $app")) {
                return DeviceAction(
                    type = ActionType.OPEN_APP,
                    target = app,
                    title = "Open $app",
                    explanation = "Launching installed application: $app",
                    requiresConfirmation = false
                )
            }
        }

        // Call intent
        if (q.startsWith("call ") || q.contains("ko call karo") || q.contains("call milao") || q.contains("dial ")) {
            val target = q.replace("call ", "")
                .replace("ko call karo", "")
                .replace("call milao", "")
                .replace("dial ", "")
                .trim()
            return DeviceAction(
                type = ActionType.DIAL_CALL,
                target = target,
                title = "Dial $target",
                explanation = "Prepare phone dialer for: $target",
                requiresConfirmation = true
            )
        }

        // SMS intent
        if (q.startsWith("sms ") || q.startsWith("message ") || q.contains("ko sms bhejo") || q.contains("sms bhejo")) {
            var target = ""
            var msg = "Assalam-o-Alaikum"
            if (q.contains(":")) {
                val parts = q.split(":")
                target = parts[0].replace("sms", "").replace("message", "").replace("bhejo", "").replace("ko", "").trim()
                msg = parts.getOrElse(1) { "Assalam-o-Alaikum" }.trim()
            } else {
                target = q.replace("sms", "").replace("message", "").replace("bhejo", "").replace("ko", "").trim()
            }
            return DeviceAction(
                type = ActionType.COMPOSE_SMS,
                target = target,
                secondaryData = msg,
                title = "Draft SMS to $target",
                explanation = "Prepare SMS with text: \"$msg\"",
                requiresConfirmation = true
            )
        }

        // Alarm intent
        if (q.contains("alarm") || q.contains("timer")) {
            return DeviceAction(
                type = ActionType.SET_ALARM,
                target = "07:00",
                title = "Set Alarm",
                explanation = "Create an alarm or reminder",
                requiresConfirmation = false
            )
        }

        // Web search intent
        if (q.startsWith("search ") || q.contains("google pe search") || q.contains("search karo")) {
            val queryToSearch = q.replace("google pe search", "")
                .replace("search karo", "")
                .replace("search ", "")
                .trim()
            return DeviceAction(
                type = ActionType.WEB_SEARCH,
                target = queryToSearch,
                title = "Search: $queryToSearch",
                explanation = "Perform Google web search for \"$queryToSearch\"",
                requiresConfirmation = false
            )
        }

        return null
    }

    suspend fun generateResponse(
        prompt: String,
        apiKeyOverride: String? = null,
        userLanguage: String = "Roman Urdu",
        conversationHistory: List<Pair<String, String>> = emptyList()
    ): Result<Pair<String, DeviceAction?>> = withContext(Dispatchers.IO) {
        val apiKey = when {
            !apiKeyOverride.isNullOrBlank() -> apiKeyOverride
            else -> try {
                val field = BuildConfig::class.java.getField("GEMINI_API_KEY")
                (field.get(null) as? String).orEmpty()
            } catch (_: Throwable) {
                ""
            }
        }

        val localAction = analyzeLocalAction(prompt)

        // Wake word handling
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.equals("Rasta AI", ignoreCase = true) ||
            cleanPrompt.equals("Hey Rasta", ignoreCase = true) ||
            cleanPrompt.equals("Hi Rasta", ignoreCase = true)) {
            val wakeResponse = when (userLanguage) {
                "Urdu" -> "جی شعیب! راستہ اے آئی حاضر ہے۔ بتائیے، میں آپ کے لیے کیا کروں؟"
                "English" -> "Yes Shoaib, Rasta AI is here! How can I assist you?"
                else -> "Ji Shoaib, Rasta AI hazir hai. Bataiye, main aap ke liye kya karun?"
            }
            return@withContext Result.success(Pair(wakeResponse, null))
        }

        // If local action detected and API key is missing, provide intelligent local action guidance
        if (apiKey.isBlank()) {
            if (localAction != null) {
                val actionMsg = when (userLanguage) {
                    "Urdu" -> "جی شعیب، میں آپ کے لیے یہ ایکشن (${localAction.title}) تیار کر رہا ہوں۔ براہ کرم تصدیق فرمائیں۔"
                    "English" -> "Yes Shoaib, I have prepared the requested action (${localAction.title}). Please confirm to execute."
                    else -> "Ji Shoaib, main aap ke liye ye device action (${localAction.title}) tayyar kar raha hoon. Neeche diye button se execute karein."
                }
                return@withContext Result.success(Pair(actionMsg, localAction))
            } else {
                return@withContext Result.failure(
                    IllegalStateException("Gemini API Key is not configured. Please set the Gemini API Key in the Admin AI Provider settings or AI Studio Secrets panel.")
                )
            }
        }

        try {
            val systemInstruction = """
                You are Rasta AI, an intelligent, respectful, and highly capable mobile AI assistant.
                The user's name is Shoaib. Always address Shoaib with warmth and respect. Do NOT unnaturally repeat the name in every single sentence.
                Respond naturally in the user's requested language ($userLanguage).
                If the user asks in Roman Urdu, respond in natural, friendly Roman Urdu (e.g. "Assalam-o-Alaikum Shoaib! Main Rasta AI hoon...").
                If the user asks in Urdu, respond in Urdu script.
                If the user asks in English, respond in English.
                Keep answers concise, direct, helpful, and formatted cleanly for mobile screens.
                If the user asks for a device action (flashlight, alarm, open app, battery, call, SMS), acknowledge it respectfully.
            """.trimIndent()

            val contentsArray = JSONArray()

            // System prompt as first turn or system instruction
            for ((role, text) in conversationHistory.takeLast(6)) {
                val turn = JSONObject()
                turn.put("role", if (role == "USER") "user" else "model")
                val parts = JSONArray().put(JSONObject().put("text", text))
                turn.put("parts", parts)
                contentsArray.put(turn)
            }

            // Current prompt
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            currentTurn.put("parts", JSONArray().put(JSONObject().put("text", prompt)))
            contentsArray.put(currentTurn)

            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
                put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction))))
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.7)
                    put("maxOutputTokens", 800)
                })
            }

            val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val modelName = "gemini-2.5-flash"
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Gemini API error (${response.code}): $responseBody"))
            }

            val rootJson = JSONObject(responseBody)
            val candidates = rootJson.optJSONArray("candidates")
            if (candidates != null && candidates.length() > 0) {
                val firstCandidate = candidates.getJSONObject(0)
                val content = firstCandidate.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val text = parts?.optJSONObject(0)?.optString("text").orEmpty()
                return@withContext Result.success(Pair(text, localAction))
            } else {
                return@withContext Result.failure(Exception("No response generated from AI model."))
            }
        } catch (e: Exception) {
            // If network fails but local action was detected, still return local action
            if (localAction != null) {
                val fallbackMsg = "Ji Shoaib, network issue ki wajah se AI connect nahi hua, lekin aap ka device action (${localAction.title}) tayyar hai."
                return@withContext Result.success(Pair(fallbackMsg, localAction))
            }
            return@withContext Result.failure(e)
        }
    }
}
