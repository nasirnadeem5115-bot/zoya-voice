package com.zoya.voice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zoya.voice.ui.theme.NeonPink

@Composable
fun SettingsScreen() {
    var userName by remember { mutableStateOf("Babe") }
    var geminiKey by remember { mutableStateOf("") }
    var grokKey by remember { mutableStateOf("") }
    var openRouterKey by remember { mutableStateOf("") }
    var showGemini by remember { mutableStateOf(false) }
    var showGrok by remember { mutableStateOf(false) }
    var showOpenRouter by remember { mutableStateOf(false) }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(24.dp))

        // User Name
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("Your Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "API Keys",
            style = MaterialTheme.typography.titleLarge,
            color = NeonPink
        )
        Text(
            text = "Keys are stored only on your device. Never shared.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Gemini
        ApiKeyField(
            label = "Gemini API Key (Live Voice – Primary)",
            value = geminiKey,
            onValueChange = { geminiKey = it },
            visible = showGemini,
            onToggleVisibility = { showGemini = !showGemini }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Grok
        ApiKeyField(
            label = "Grok (xAI) API Key",
            value = grokKey,
            onValueChange = { grokKey = it },
            visible = showGrok,
            onToggleVisibility = { showGrok = !showGrok }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // OpenRouter
        ApiKeyField(
            label = "OpenRouter API Key",
            value = openRouterKey,
            onValueChange = { openRouterKey = it },
            visible = showOpenRouter,
            onToggleVisibility = { showOpenRouter = !showOpenRouter }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // TODO: Save to DataStore
                saved = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
        ) {
            Text(if (saved) "Saved ✓" else "Save Settings")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Gemini is used for real-time voice-to-voice. Grok & OpenRouter are ready for future upgrades.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ApiKeyField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    visible: Boolean,
    onToggleVisibility: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = onToggleVisibility) {
                Icon(
                    imageVector = if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = null
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    )
}