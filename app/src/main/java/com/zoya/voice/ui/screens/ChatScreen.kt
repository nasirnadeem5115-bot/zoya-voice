package com.zoya.voice.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zoya.voice.data.SettingsRepository
import com.zoya.voice.domain.model.ChatMessage
import com.zoya.voice.ui.theme.NeonPink
import com.zoya.voice.voice.ChatClient
import kotlinx.coroutines.launch

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    val grokKey by settings.grokKey.collectAsState(initial = "")
    val geminiKey by settings.geminiKey.collectAsState(initial = "")
    val openRouterKey by settings.openRouterKey.collectAsState(initial = "")

    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val chatClient = remember { ChatClient() }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty() || isLoading) {
            listState.animateScrollToItem(
                (messages.size + if (isLoading) 1 else 0).coerceAtLeast(0)
            )
        }
    }

    fun sendMessage() {
        val text = input.trim()
        if (text.isBlank() || isLoading) return

        if (grokKey.isBlank() && geminiKey.isBlank() && openRouterKey.isBlank()) {
            errorText = "Settings mein Grok ya Gemini API key daalo"
            return
        }

        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            text = text,
            isUser = true
        )
        messages = messages + userMsg
        input = ""
        errorText = null
        isLoading = true

        scope.launch {
            val result = chatClient.send(
                userText = text,
                history = messages.dropLast(1), // exclude the message we just added
                grokKey = grokKey,
                geminiKey = geminiKey,
                openRouterKey = openRouterKey
            )
            isLoading = false
            result.fold(
                onSuccess = { reply ->
                    messages = messages + ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        text = reply,
                        isUser = false
                    )
                },
                onFailure = { e ->
                    errorText = e.message ?: "Failed to get reply"
                    messages = messages + ChatMessage(
                        id = (System.currentTimeMillis() + 1).toString(),
                        text = "Oops… ${e.message ?: "something went wrong"}. Key check karo Settings mein.",
                        isUser = false
                    )
                }
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Provider hint
        val providerHint = when {
            grokKey.isNotBlank() -> "Powered by Grok · Zoya mode"
            geminiKey.isNotBlank() -> "Powered by Gemini · Zoya mode"
            openRouterKey.isNotBlank() -> "Powered by OpenRouter · Zoya mode"
            else -> "Add Grok API key in Settings for full chat"
        }
        Text(
            text = providerHint,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (messages.isEmpty() && !isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Ask Zoya anything… like Grok, but with attitude 😉",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (msg.isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .background(
                                color = if (msg.isUser) NeonPink
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (msg.isUser) 18.dp else 4.dp,
                                    bottomEnd = if (msg.isUser) 4.dp else 18.dp
                                )
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (msg.isUser) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(18.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Zoya is typing…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        if (errorText != null) {
            Text(
                text = errorText!!,
                color = NeonPink,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask Zoya anything…") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                enabled = !isLoading
            )
            Spacer(modifier = Modifier.width(8.dp))
            FloatingActionButton(
                onClick = { sendMessage() },
                containerColor = NeonPink,
                modifier = Modifier.size(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
