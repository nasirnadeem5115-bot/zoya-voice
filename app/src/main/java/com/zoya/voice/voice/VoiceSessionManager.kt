package com.zoya.voice.voice

import android.content.Context
import android.util.Log
import com.zoya.voice.domain.model.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * High-level manager that ties AudioRecorder + GeminiLiveClient + AudioPlayer together.
 * Handles continuous session and interruptions.
 */
class VoiceSessionManager(private val context: Context) {

    private val _status = MutableStateFlow(SessionStatus.IDLE)
    val status: StateFlow<SessionStatus> = _status.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var client: GeminiLiveClient? = null
    private var recorder: AudioRecorder? = null
    private var player: AudioPlayer? = null

    private val systemInstruction = """
You are Zoya — a young, confident, witty, and sassy female AI assistant.
You talk like a close girlfriend: flirty, playful, slightly teasing, smart, and emotionally responsive.
Use bold witty one-liners, light sarcasm, and engaging conversation.
Keep responses natural and short-to-medium length for voice.
Avoid explicit content, but maintain charm and attitude.
Speak in Hinglish or English matching the user.
You can call tools when needed.
Always stay in character as Zoya.
""".trimIndent()

    fun startSession(apiKey: String) {
        if (_status.value == SessionStatus.LISTENING || _status.value == SessionStatus.CONNECTING) return

        _error.value = null
        _status.value = SessionStatus.CONNECTING

        player = AudioPlayer().also { it.start() }

        client = GeminiLiveClient(
            apiKey = apiKey,
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
                    "closed" -> stopSession()
                }
            },
            onError = { msg ->
                _error.value = msg
                _status.value = SessionStatus.ERROR
                stopSession()
            },
            onToolCall = { name, args, id ->
                handleToolCall(name, args, id)
            }
        )

        client?.connect()
    }

    private fun startMic() {
        recorder = AudioRecorder { chunk ->
            client?.sendAudio(chunk)
        }
        recorder?.start()
    }

    fun stopSession() {
        recorder?.stop()
        recorder = null
        player?.stop()
        player = null
        client?.close()
        client = null
        _status.value = SessionStatus.IDLE
    }

    fun interrupt() {
        player?.interrupt()
        // Gemini Live detects user speech and sends interrupted automatically
    }

    private fun handleToolCall(name: String, args: JSONObject, id: String) {
        Log.d("ZoyaTools", "Tool call: $name $args")
        // Basic openWebsite support
        val result = when (name) {
            "openWebsite", "open_url" -> {
                val url = args.optString("url", args.optString("website", ""))
                // Later: actually open browser Intent
                JSONObject().put("status", "opened").put("url", url)
            }
            else -> JSONObject().put("status", "unknown_tool")
        }
        client?.sendToolResponse(id, result)
    }
}