package com.zoya.voice.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zoya.voice.data.TriggerRepository
import com.zoya.voice.domain.model.Trigger
import com.zoya.voice.ui.theme.NeonPink
import kotlinx.coroutines.launch

@Composable
fun TriggersScreen() {
    val context = LocalContext.current
    val repo = remember { TriggerRepository(context) }
    val scope = rememberCoroutineScope()

    val triggers by repo.triggers.collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Triggers",
                style = MaterialTheme.typography.headlineMedium
            )
            IconButton(onClick = {
                // Simple add for now – full dialog in later step
                val newTrigger = Trigger(
                    id = System.currentTimeMillis().toString(),
                    name = "New Trigger",
                    phrase = "hey zoya",
                    action = "custom",
                    enabled = true
                )
                scope.launch {
                    repo.save(triggers + newTrigger)
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = NeonPink)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (triggers.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No triggers yet. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(triggers, key = { it.id }) { trigger ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Switch(
                                checked = trigger.enabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        val updated = triggers.map {
                                            if (it.id == trigger.id) it.copy(enabled = enabled) else it
                                        }
                                        repo.save(updated)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = NeonPink)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(trigger.name, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "\"${trigger.phrase}\"",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    repo.save(triggers.filter { it.id != trigger.id })
                                }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}