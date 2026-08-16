package com.example.regibazarapp.service

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.regibazarapp.data.models.DriverCamiRequest
import com.example.regibazarapp.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * ══════════════════════════════════════════════════════════════
 *  CamiManager – Voice I/O manager for the C.A.M.I. copilot
 *
 *  - Input  : Android SpeechRecognizer (mic → text)
 *  - Output : Android TextToSpeech     (text → audio, es-MX)
 *  - Backend: POST api/driver/{token}/cami-command
 * ══════════════════════════════════════════════════════════════
 */

enum class CamiState {
    IDLE, LISTENING, PROCESSING, SPEAKING, ERROR
}

class CamiManager(
    private val context: Context,
    private val scope: CoroutineScope
) {

    // ── Public callbacks ──────────────────────────────────────
    var onStateChange: (CamiState) -> Unit = {}
    var onTranscript: (String) -> Unit = {}
    var onResponse: (String) -> Unit = {}
    var onError: (String) -> Unit = {}

    // ── Audio focus (para ducking música Bluetooth) ───────────
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    // ── Internal state ────────────────────────────────────────
    private var currentState: CamiState = CamiState.IDLE
        set(value) {
            field = value
            onStateChange(value)
        }

    private var driverToken: String = ""

    // SpeechRecognizer MUST be created and used on the main thread
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null

    // TextToSpeech
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private val TTS_UTTERANCE_ID = "cami_utterance"

    init {
        initTts()
        mainHandler.post { initSpeechRecognizer() }
    }

    // ── Public API ────────────────────────────────────────────

    fun setToken(token: String) {
        driverToken = token
    }

    /**
     * Toggle: if IDLE or ERROR → start listening; if LISTENING → stop.
     */
    fun toggleListening() {
        when (currentState) {
            CamiState.IDLE, CamiState.ERROR -> startListening()
            CamiState.LISTENING -> stopListening()
            else -> { /* ignore taps while PROCESSING or SPEAKING */ }
        }
    }

    /**
     * Anuncia un texto por TTS sin necesidad de que el usuario active el micrófono.
     * Duckea la música del estéreo / Bluetooth para que se escuche claramente.
     * Ideal para anuncios de proximidad automáticos.
     */
    fun announce(text: String) {
        if (!ttsReady || tts == null) return
        mainHandler.post {
            requestAudioFocus()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "cami_announce")
        }
    }

    private fun requestAudioFocus() {
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .build()
        audioManager.requestAudioFocus(focusRequest)
        audioFocusRequest = focusRequest
    }

    fun destroy() {
        mainHandler.post {
            speechRecognizer?.destroy()
            speechRecognizer = null
        }
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    // ── TTS init ──────────────────────────────────────────────

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val locale = Locale("es", "MX")
                val result = tts?.setLanguage(locale)
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED
            } else {
                ttsReady = false
            }
        }

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == TTS_UTTERANCE_ID || utteranceId == "cami_announce") {
                    currentState = CamiState.IDLE
                }
                // Liberar audio focus para que la música del estéreo/Bluetooth se reanude
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            }

            @Deprecated("Deprecated in API 21")
            override fun onError(utteranceId: String?) {
                currentState = CamiState.IDLE
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                currentState = CamiState.IDLE
            }
        })
    }

    // ── SpeechRecognizer init (must run on main thread) ───────

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition not available on this device")
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                currentState = CamiState.LISTENING
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO             -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT            -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK           -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT   -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH          -> "No speech match found"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY   -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER            -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT    -> "No speech input"
                    else                                     -> "Unknown recognition error ($error)"
                }
                currentState = CamiState.ERROR
                onError(message)
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()
                if (text.isNullOrBlank()) {
                    currentState = CamiState.ERROR
                    onError("Could not understand speech")
                    return
                }
                onTranscript(text)
                currentState = CamiState.PROCESSING
                sendToCami(text)
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // ── Listening control ─────────────────────────────────────

    private fun startListening() {
        mainHandler.post {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            }
            // Set state before startListening so UI updates immediately
            currentState = CamiState.LISTENING
            speechRecognizer?.startListening(intent)
        }
    }

    private fun stopListening() {
        mainHandler.post {
            speechRecognizer?.stopListening()
            currentState = CamiState.IDLE
        }
    }

    // ── API call ──────────────────────────────────────────────

    private fun sendToCami(text: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val response = NetworkModule.api.camiCommand(
                    driverToken = driverToken,
                    request = DriverCamiRequest(commandText = text)
                )
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val body = response.body()
                        val replyText = body?.respuestaCami ?: ""
                        onResponse(replyText)
                        if (replyText.isNotBlank()) {
                            speak(replyText)
                        } else {
                            currentState = CamiState.IDLE
                        }
                    } else {
                        currentState = CamiState.ERROR
                        onError("API error: ${response.code()} ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    currentState = CamiState.ERROR
                    onError("Network error: ${e.localizedMessage ?: e.message ?: "Unknown error"}")
                }
            }
        }
    }

    // ── TTS playback ──────────────────────────────────────────

    private fun speak(text: String) {
        if (!ttsReady || tts == null) {
            // TTS not ready — still report idle so UI doesn't get stuck
            currentState = CamiState.IDLE
            onError("Text-to-speech not ready")
            return
        }
        currentState = CamiState.SPEAKING
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, TTS_UTTERANCE_ID)
    }
}
