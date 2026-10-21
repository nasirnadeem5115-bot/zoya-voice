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
        // Current native audio model (update if Google renames)
        private const val MODEL = "models/gemini-2.5-flash-native-audio-preview-12-2025"
        private const val WS_URL =
            "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService.BidiGenerateContent"
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
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onStatus("closed")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                onError(t.message ?: "Connection failed")
            }
        })
    }

    private fun sendSetup() {
        val setup = JSONObject().apply {
            put("setup", JSONObject().apply {
                put("model", MODEL)
                put("generationConfig", JSONObject().apply {
                    put("responseModalities", JSONArray().put("AUDIO"))
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                })
            })
        }
        webSocket?.send(setup.toString())
    }

    fun sendAudio(pcm16k: ByteArray) {
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

            if (json.has("setupComplete")) {
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
                        if (inlineData != null) {
                            val mime = inlineData.optString("mimeType", "")
                            val data = inlineData.optString("data", "")
                            if (mime.contains("audio") && data.isNotEmpty()) {
                                val pcm = Base64.decode(data, Base64.DEFAULT)
                                onAudio(pcm)
                            }
                        }
                    }
                }

                if (serverContent.optBoolean("turnComplete", false)) {
                    onStatus("turnComplete")
                }
            }

            val toolCall = json.optJSONObject("toolCall")
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
