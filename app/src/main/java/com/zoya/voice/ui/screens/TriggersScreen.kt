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
import androidx.compose.ui.unit.dp
import com.zoya.voice.ui.theme.NeonPink

data class Trigger(
    val id: String,
    val name: String,
    val phrase: String,
    val action: String,
    var enabled: Boolean
)

@Composable
fun TriggersScreen() {
    var triggers by remember {
        mutableStateOf(
            listOf(
                Trigger("1", "Open YouTube", "open youtube", "openWebsite:https://youtube.com", true),
                Trigger("2", "Search Google", "search for", "search", true),
                Trigger("3", "Set Reminder", "remind me", "reminder", true)
            )
        )
    }

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
            IconButton(onClick = { /* TODO: Add dialog */ }) {
                Icon(Icons.Default.Add, contentDescription = "Add", tint = NeonPink)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

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
                                triggers = triggers.map {
                                    if (it.id == trigger.id) it.copy(enabled = enabled) else it
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
                            triggers = triggers.filter { it.id != trigger.id }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}