package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

class RastaVoiceManager(
    private val context: Context,
    private val onSpeechRecognized: (String) -> Unit,
    private val onWakeWordDetected: () -> Unit
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    private val _lastSpokenText = MutableStateFlow("")
    val lastSpokenText: StateFlow<String> = _lastSpokenText.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private var currentLanguage = "Roman Urdu"
    private var speechRate = 1.0f
    private var speechPitch = 1.0f

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            _voiceState.value = VoiceState.ERROR
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            updateTtsLocale(currentLanguage)
            tts?.setSpeechRate(speechRate)
            tts?.setPitch(speechPitch)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = VoiceState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    if (_voiceState.value == VoiceState.SPEAKING) {
                        _voiceState.value = VoiceState.IDLE
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _voiceState.value = VoiceState.IDLE
                }
            })
        }
    }

    fun configureSettings(language: String, rate: Float, pitch: Float) {
        currentLanguage = language
        speechRate = rate
        speechPitch = pitch
        if (isTtsReady) {
            updateTtsLocale(language)
            tts?.setSpeechRate(rate)
            tts?.setPitch(pitch)
        }
    }

    private fun updateTtsLocale(lang: String) {
        val locale = when (lang) {
            "Urdu" -> Locale("ur", "PK")
            "Roman Urdu", "Hindi" -> Locale("hi", "IN")
            "Arabic" -> Locale("ar")
            "Spanish" -> Locale("es")
            "French" -> Locale.FRENCH
            "German" -> Locale.GERMAN
            "Chinese" -> Locale.CHINESE
            else -> Locale.US
        }

        val available = tts?.isLanguageAvailable(locale)
        if (available == TextToSpeech.LANG_AVAILABLE || available == TextToSpeech.LANG_COUNTRY_AVAILABLE) {
            tts?.language = locale
        } else {
            tts?.language = Locale.US
        }
    }

    fun speak(text: String) {
        if (!isTtsReady || text.isBlank()) return
        stopListening()
        stopSpeaking()

        _lastSpokenText.value = text
        _voiceState.value = VoiceState.SPEAKING

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "rasta_msg_${System.currentTimeMillis()}")
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, params.getString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))
    }

    fun stopSpeaking() {
        tts?.stop()
        if (_voiceState.value == VoiceState.SPEAKING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun startListening() {
        stopSpeaking()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.ERROR
            return
        }

        try {
            if (speechRecognizer == null) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _voiceState.value = VoiceState.LISTENING
                }

                override fun onBeginningOfSpeech() {
                    _voiceState.value = VoiceState.LISTENING
                }

                override fun onRmsChanged(rmsdB: Float) {
                    _rmsLevel.value = (rmsdB / 10f).coerceIn(0f, 1f)
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    _voiceState.value = VoiceState.PROCESSING
                }

                override fun onError(error: Int) {
                    _voiceState.value = VoiceState.IDLE
                    _rmsLevel.value = 0f
                }

                override fun onResults(results: Bundle?) {
                    _voiceState.value = VoiceState.IDLE
                    _rmsLevel.value = 0f
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim().orEmpty()
                    if (spoken.isNotBlank()) {
                        checkWakePhraseAndDispatch(spoken)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spoken = matches?.firstOrNull()?.trim().orEmpty()
                    if (spoken.isNotBlank() && isWakePhrase(spoken)) {
                        stopListening()
                        onWakeWordDetected()
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                val tag = when (currentLanguage) {
                    "Urdu" -> "ur-PK"
                    "Roman Urdu", "Hindi" -> "hi-IN"
                    "Arabic" -> "ar-SA"
                    "Spanish" -> "es-ES"
                    "French" -> "fr-FR"
                    "German" -> "de-DE"
                    "Chinese" -> "zh-CN"
                    else -> "en-US"
                }
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
            }

            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.LISTENING
        } catch (e: Exception) {
            _voiceState.value = VoiceState.ERROR
        }
    }

    private fun isWakePhrase(text: String): Boolean {
        val lower = text.lowercase().trim()
        return lower == "rasta ai" || lower == "hey rasta" || lower == "hi rasta" ||
                lower.startsWith("rasta ai ") || lower.startsWith("hey rasta ") || lower.startsWith("hi rasta ")
    }

    private fun checkWakePhraseAndDispatch(spoken: String) {
        if (isWakePhrase(spoken)) {
            val remaining = spoken
                .replace("rasta ai", "", ignoreCase = true)
                .replace("hey rasta", "", ignoreCase = true)
                .replace("hi rasta", "", ignoreCase = true)
                .trim()

            if (remaining.isBlank()) {
                onWakeWordDetected()
            } else {
                onSpeechRecognized(remaining)
            }
        } else {
            onSpeechRecognized(spoken)
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (_: Exception) {}
        _rmsLevel.value = 0f
        if (_voiceState.value == VoiceState.LISTENING || _voiceState.value == VoiceState.PROCESSING) {
            _voiceState.value = VoiceState.IDLE
        }
    }

    fun destroy() {
        stopListening()
        stopSpeaking()
        speechRecognizer?.destroy()
        speechRecognizer = null
        tts?.shutdown()
        tts = null
    }
}
