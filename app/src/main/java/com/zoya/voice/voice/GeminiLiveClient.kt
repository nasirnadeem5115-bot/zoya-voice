package com.zoya.voice.voice

import android.util.Base64
import android.util.Log
import okhttp3.*
import okio.ByteString
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Gemini Live API client using OkHttp WebSocket.
 * Sends 16 kHz PCM, receives 24 kHz PCM + tool calls.
 * Updated for gemini-3.8-live (Oct 2026).
 */
class GeminiLiveClient(
    private val apiKey: String,
    private val systemInstruction: String,
    private val onAudio: (ByteArray) -> Unit,
    private val onStatus: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onToolCall: ((name: String, args: JSONObject, id: String) -> Unit)? = null
) {
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "GeminiLive"
        // Current recommended Live model (as of Oct 2026)
        private const val MODEL = "models/gemini-3.8-live"
        // v1beta is the stable Live endpoint
        private const val WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent"
    }

    fun connect() {
        val request = Request.Builder()
            .url("$WS_URL?key=$apiKey")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket opened")
                sendSetup()
                onStatus("connected")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                handleMessage(text)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {}

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Closed: $code $reason")
                onStatus("closed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}", t)
                val body = try { response?.body?.string() } catch (_: Exception) { null }
                val msg = when {
                    body?.isNotBlank() == true -> body
                    t.message?.contains("401") == true || response?.code == 401 ->
                        "Invalid API key (401)"
                    t.message?.contains("403") == true || response?.code == 403 ->
                        "API key not allowed for Live / region blocked (403)"
                    t.message?.contains("404") == true || response?.code == 404 ->
                        "Model not found — check Live access"
                    else -> t.message ?: "Connection failed"
                }
                onError(msg)
            }
        })
    }

    private fun sendSetup() {
        val setup = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", MODEL)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                    // Optional: pick a voice (Aoede, Charon, Fenrir, Kore, Puck)
                    put("speechConfig", JSONObject().apply {
                        put("voiceConfig", JSONObject().apply {
                            put("prebuiltVoiceConfig", JSONObject().apply {
                                put("voiceName", "Kore")
                            })
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
            })
        }
        webSocket?.send(setup.toString())
        Log.d(TAG, "Setup sent for $MODEL")
    }

    fun sendAudio(pcm16k: ByteArray) {
        if (webSocket == null) return
        val b64 = Base64.encodeToString(pcm16k, Base64.NO_WRAP)
        val msg = JSONObject().apply {
            put("realtimeInput", JSONObject().apply {
                put("audio", JSONObject().apply {
                    put("mimeType", "audio/pcm;rate=16000")
                    put("data", b64)
                })
            })
        }
        webSocket?.send(msg.toString())
    }

    fun sendToolResponse(id: String, result: JSONObject) {
        val msg = JSONObject().apply {
            put("toolResponse", JSONObject().apply {
                put("functionResponses", JSONArray().put(JSONObject().apply {
                    put("id", id)
                    put("response", result)
                }))
            })
        }
        webSocket?.send(msg.toString())
    }

    private fun handleMessage(text: String) {
        try {
            val json = JSONObject(text)

            // Server error (e.g. invalid model, quota, key)
            if (json.has("error")) {
                val err = json.optJSONObject("error")
                val message = err?.optString("message") ?: json.toString()
                Log.e(TAG, "Server error: $message")
                onError(message)
                return
            }

            if (json.has("setupComplete")) {
                Log.d(TAG, "Setup complete — session ready")
                onStatus("ready")
                return
            }

            val serverContent = json.optJSONObject("serverContent")
            if (serverContent != null) {
                if (serverContent.optBoolean("interrupted", false)) {
                    onStatus("interrupted")
                    return
                }

                val modelTurn = serverContent.optJSONObject("modelTurn")
                modelTurn?.optJSONArray("parts")?.let { parts ->
                    for (i in 0 until parts.length()) {
                        val part = parts.getJSONObject(i)
                        val inlineData = part.optJSONObject("inlineData")
                            ?: part.optJSONObject("inline_data")
                        if (inlineData != null) {
                            val mime = inlineData.optString("mimeType", "")
                                .ifEmpty { inlineData.optString("mime_type", "") }
                            val data = inlineData.optString("data", "")
                            if ((mime.contains("audio") || mime.isEmpty()) && data.isNotEmpty()) {
                                val pcm = Base64.decode(data, Base64.DEFAULT)
                                onAudio(pcm)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false) ||
                    serverContent.optBoolean("turn_complete", false)
                ) {
                    onStatus("turnComplete")
                }
            }

            val toolCall = json.optJSONObject("toolCall")
                ?: json.optJSONObject("tool_call")
            toolCall?.optJSONArray("functionCalls")?.let { calls ->
                for (i in 0 until calls.length()) {
                    val call = calls.getJSONObject(i)
                    val name = call.optString("name")
                    val id = call.optString("id")
                    val args = call.optJSONObject("args") ?: JSONObject()
                    onToolCall?.invoke(name, args, id)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse error", e)
        }
    }

    fun close() {
        webSocket?.close(1000, "bye")
        webSocket = null
    }
}
