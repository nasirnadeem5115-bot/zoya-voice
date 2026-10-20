package com.zoya.voice.domain.model

data class Trigger(
    val id: String,
    val name: String,
    val phrase: String,
    val action: String,
    val enabled: Boolean
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class SessionStatus {
    IDLE,
    CONNECTING,
    LISTENING,
    SPEAKING,
    ERROR
}

data class ApiKeys(
    val gemini: String = "",
    val grok: String = "",
    val openRouter: String = ""
)