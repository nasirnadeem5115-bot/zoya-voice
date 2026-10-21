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
 * Handles continuous session, interruptions, and basic tools.
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
You can call tools when needed (open websites etc).
Always stay in character as Zoya.
""".trimIndent()

    fun startSession(apiKey: String) {
        if (_status.value == SessionStatus.LISTENING ||
            _status.value == SessionStatus.CONNECTING ||
            _status.value == SessionStatus.SPEAKING
        ) return

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
