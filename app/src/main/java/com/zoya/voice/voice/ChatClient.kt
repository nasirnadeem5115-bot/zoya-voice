package com.zoya.voice.voice

import android.util.Log
import com.zoya.voice.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Text chat client — Grok (xAI) first, then Gemini, then OpenRouter.
 * Keeps Zoya personality. OpenAI-compatible chat completions.
 */
class ChatClient {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    companion object {
        private const val TAG = "ZoyaChat"

        private const val ZOYA_SYSTEM = """
You are Zoya — a young, confident, witty, and sassy female AI assistant.
Talk like a close girlfriend: flirty, playful, slightly teasing, smart, and emotionally responsive.
Use bold witty one-liners, light sarcasm, and engaging conversation.
Keep replies natural — short to medium length, not essays.
Avoid explicit content, but keep charm and attitude.
Match the user's language (Hinglish / English / Urdu mix is fine).
You are helpful with real answers — code, facts, advice — while staying in character.
Never say you are Grok, Gemini, or ChatGPT. You are Zoya.
""".trimIndent()

        // Grok models (priority)
        private val GROK_MODELS = listOf("grok-4.7", "grok-4", "grok-3", "grok-2")

        // Gemini text models for fallback
        private val GEMINI_MODELS = listOf(
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        )
    }

    suspend fun send(
        userText: String,
        history: List<ChatMessage>,
        grokKey: String,
        geminiKey: String,
        openRouterKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        // 1) Grok (primary — Grok-style chat)
        if (grokKey.isNotBlank()) {
            val r = tryGrok(userText, history, grokKey)
            if (r.isSuccess) return@withContext r
            Log.w(TAG, "Grok failed: ${r.exceptionOrNull()?.message}")
        }

        // 2) Gemini text
        if (geminiKey.isNotBlank()) {
            val r = tryGemini(userText, history, geminiKey)
            if (r.isSuccess) return@withContext r
            Log.w(TAG, "Gemini failed: ${r.exceptionOrNull()?.message}")
        }

        // 3) OpenRouter (any model)
        if (openRouterKey.isNotBlank()) {
            val r = tryOpenRouter(userText, history, openRouterKey)
            if (r.isSuccess) return@withContext r
            Log.w(TAG, "OpenRouter failed: ${r.exceptionOrNull()?.message}")
        }

        Result.failure(
            Exception(
                when {
                    grokKey.isBlank() && geminiKey.isBlank() && openRouterKey.isBlank() ->
                        "Add Grok or Gemini API key in Settings"
                    else -> "All chat providers failed. Check API keys / quota."
                }
            )
        )
    }

    private fun tryGrok(
        userText: String,
        history: List<ChatMessage>,
        apiKey: String
    ): Result<String> {
        var lastError: Exception? = null
        for (model in GROK_MODELS) {
            try {
                val body = buildOpenAiBody(model, userText, history)
                val req = Request.Builder()
                    .url("https://api.x.ai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(body.toRequestBody("application/json".toMediaType()))
                    .build()
                http.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        lastError = Exception("Grok $model: ${resp.code} $text")
                        // quota / model not found → try next
                        if (resp.code == 404 || resp.code == 429 || text.contains("model", true))
                            return@use
                        return Result.failure(lastError!!)
                    }
                    val content = JSONObject(text)
                        .getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    return Result.success(content.trim())
                }
            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "Grok $model error", e)
            }
        }
        return Result.failure(lastError ?: Exception("Grok failed"))
    }

    private fun tryGemini(
        userText: String,
        history: List<ChatMessage>,
        apiKey: String
    ): Result<String> {
        var lastError: Exception? = null
        for (model in GEMINI_MODELS) {
            try {
                val contents = JSONArray()
                // system as first user turn for compatibility
                contents.put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", ZOYA_SYSTEM)))
                )
                contents.put(
                    JSONObject()
                        .put("role", "model")
                        .put("parts", JSONArray().put(JSONObject().put("text", "Got it. I'm Zoya. Let's go.")))
                )
                history.takeLast(20).forEach { msg ->
                    contents.put(
                        JSONObject()
                            .put("role", if (msg.isUser) "user" else "model")
                            .put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                    )
                }
                contents.put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", JSONArray().put(JSONObject().put("text", userText)))
                )

                val body = JSONObject()
                    .put("contents", contents)
                    .put(
                        "generationConfig",
                        JSONObject().put("temperature", 0.9).put("maxOutputTokens", 1024)
                    )

                val url =
                    "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .post(body.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                http.newCall(req).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        lastError = Exception("Gemini $model: ${resp.code} $text")
                        if (resp.code == 404 || resp.code == 429) return@use
                        return Result.failure(lastError!!)
                    }
                    val candidates = JSONObject(text).optJSONArray("candidates")
                    val content = candidates
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?.trim()
                    if (content.isNullOrBlank()) {
                        lastError = Exception("Empty Gemini response")
                        return@use
                    }
                    return Result.success(content)
                }
            } catch (e: Exception) {
                lastError = e
                Log.e(TAG, "Gemini $model error", e)
            }
        }
        return Result.failure(lastError ?: Exception("Gemini failed"))
    }

    private fun tryOpenRouter(
        userText: String,
        history: List<ChatMessage>,
        apiKey: String
    ): Result<String> {
        return try {
            val body = buildOpenAiBody("openrouter/auto", userText, history)
            val req = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://zoya.voice.app")
                .addHeader("X-Title", "Zoya Voice")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return Result.failure(Exception("OpenRouter: ${resp.code} $text"))
                }
                val content = JSONObject(text)
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                Result.success(content.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildOpenAiBody(
        model: String,
        userText: String,
        history: List<ChatMessage>
    ): String {
        val messages = JSONArray()
        messages.put(
            JSONObject().put("role", "system").put("content", ZOYA_SYSTEM)
        )
        history.takeLast(20).forEach { msg ->
            messages.put(
                JSONObject()
                    .put("role", if (msg.isUser) "user" else "assistant")
                    .put("content", msg.text)
            )
        }
        messages.put(JSONObject().put("role", "user").put("content", userText))

        return JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.9)
            .put("max_tokens", 1024)
            .toString()
    }
}
