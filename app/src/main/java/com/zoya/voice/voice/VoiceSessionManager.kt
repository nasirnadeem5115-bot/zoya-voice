package com.zoya.voice.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.zoya.voice.domain.model.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * High-level manager: AudioRecorder + GeminiLiveClient + AudioPlayer.
 * Automatic model fallback when quota / model unavailable.
 * Chain: 3.8 → 3.1 → 2.5 → live-2.5 → 2.0
 */
class VoiceSessionManager(private val context: Context) {

    private val _status = MutableStateFlow(SessionStatus.IDLE)
    val status: StateFlow<SessionStatus> = _status.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _activeModel = MutableStateFlow<String?>(null)
    val activeModel: StateFlow<String?> = _activeModel.asStateFlow()

    private var client: GeminiLiveClient? = null
    private var recorder: AudioRecorder? = null
    private var player: AudioPlayer? = null

    private var currentApiKey: String = ""
    private var modelIndex: Int = 0

    private val systemInstruction = """
You are Zoya — a young, confident, witty, and sassy female AI assistant.
You talk like a close girlfriend: flirty, playful, slightly teasing, smart, and emotionally responsive.
Use bold witty one-liners, light sarcasm, and engaging conversation.
Keep responses natural and short-to-medium length for voice.
Avoid explicit content, but maintain charm and attitude.
Speak in Hinglish or English matching the user.
You can call tools when needed (open websites etc).
Always stay in character as Zoya.
""".trimIndent()

    fun startSession(apiKey: String) {
        if (_status.value == SessionStatus.LISTENING ||
            _status.value == SessionStatus.CONNECTING ||
            _status.value == SessionStatus.SPEAKING
        ) return

        currentApiKey = apiKey
        modelIndex = 0
        _error.value = null
        connectWithCurrentModel()
    }

    private fun connectWithCurrentModel() {
        val models = GeminiLiveClient.FALLBACK_MODELS
        if (modelIndex >= models.size) {
            _error.value = "All models quota exhausted. Try again later."
            _status.value = SessionStatus.ERROR
            _activeModel.value = null
            return
        }

        val model = models[modelIndex]
        _activeModel.value = model.removePrefix("models/")
        _status.value = SessionStatus.CONNECTING
        Log.d("ZoyaSession", "Trying model[$modelIndex]: $model")

        // Clean previous client only (keep player if already started)
        client?.close()
        client = null
        recorder?.stop()
        recorder = null

        if (player == null) {
            player = AudioPlayer().also { it.start() }
        }

        client = GeminiLiveClient(
            apiKey = currentApiKey,
            model = model,
            systemInstruction = systemInstruction,
            onAudio = { pcm ->
                _status.value = SessionStatus.SPEAKING
                player?.play(pcm)
            },
            onStatus = { s ->
                when (s) {
                    "ready", "connected" -> {
                        _status.value = SessionStatus.LISTENING
                        startMic()
                    }
                    "interrupted" -> {
                        player?.interrupt()
                        _status.value = SessionStatus.LISTENING
                    }
                    "turnComplete" -> {
                        _status.value = SessionStatus.LISTENING
                    }
                    "closed" -> {
                        // only mark idle if we are not mid-fallback
                    }
                }
            },
            onError = { msg ->
                _error.value = msg
                _status.value = SessionStatus.ERROR
                cleanupSession()
            },
            onQuotaExhausted = {
                Log.w("ZoyaSession", "Quota/unavailable on $model → fallback")
                modelIndex++
                if (modelIndex < models.size) {
                    // brief delay then try next model
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        connectWithCurrentModel()
                    }, 400)
                } else {
                    _error.value = "All models quota exhausted. Try again later."
                    _status.value = SessionStatus.ERROR
                    _activeModel.value = null
                    cleanupSession()
                }
            },
            onToolCall = { name, args, id ->
                handleToolCall(name, args, id)
            }
        )

        client?.connect()
    }

    private fun startMic() {
        if (recorder != null) return
        recorder = AudioRecorder { chunk ->
            client?.sendAudio(chunk)
        }
        recorder?.start()
    }

    private fun cleanupSession() {
        recorder?.stop()
        recorder = null
        player?.stop()
        player = null
        client?.close()
        client = null
    }

    fun stopSession() {
        cleanupSession()
        _status.value = SessionStatus.IDLE
        _activeModel.value = null
        modelIndex = 0
    }

    fun interrupt() {
        player?.interrupt()
    }

    private fun handleToolCall(name: String, args: JSONObject, id: String) {
        Log.d("ZoyaTools", "Tool: $name $args")
        val result = when (name.lowercase()) {
            "openwebsite", "open_url", "open_website" -> {
                val url = args.optString("url")
                    .ifEmpty { args.optString("website") }
                    .ifEmpty { args.optString("link") }
                if (url.isNotBlank()) {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                        JSONObject().put("status", "opened").put("url", url)
                    } catch (e: Exception) {
                        JSONObject().put("status", "error").put("message", e.message)
                    }
                } else {
                    JSONObject().put("status", "error").put("message", "No URL provided")
                }
            }
            else -> JSONObject().put("status", "unknown_tool").put("name", name)
        }
        client?.sendToolResponse(id, result)
    }
}
