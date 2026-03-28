package com.stavros.graham

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onSave: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { Settings(context) }

    var serverUrl by remember { mutableStateOf(settings.serverUrl) }
    var bodyTemplate by remember { mutableStateOf(settings.bodyTemplate) }
    var ttsSpeed by remember { mutableFloatStateOf(settings.ttsSpeed) }
    var tonesEnabled by remember { mutableStateOf(settings.tonesEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
        )

        OutlinedTextField(
            value = serverUrl,
            onValueChange = { serverUrl = it },
            label = { Text("Server URL") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = bodyTemplate,
            onValueChange = { bodyTemplate = it },
            label = { Text("Body template") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Text(text = "TTS speed: ${"%.2f".format(ttsSpeed)}x")
        Slider(
            value = ttsSpeed,
            onValueChange = { raw ->
                // Snap to the nearest 0.05 increment so the displayed value matches
                // what will actually be stored.
                val steps = ((raw - 0.8f) / 0.05f).roundToInt()
                ttsSpeed = 0.8f + steps * 0.05f
            },
            valueRange = 0.8f..1.8f,
            // 19 internal steps = 20 positions of 0.05 across the 0.8–1.8 range.
            steps = 19,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Enable tones")
            Switch(
                checked = tonesEnabled,
                onCheckedChange = { tonesEnabled = it },
            )
        }

        Button(
            onClick = {
                settings.serverUrl = serverUrl
                settings.bodyTemplate = bodyTemplate
                settings.ttsSpeed = ttsSpeed
                settings.tonesEnabled = tonesEnabled
                onSave()
            },
            enabled = serverUrl.isNotEmpty() && bodyTemplate.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Save")
        }
    }
}
