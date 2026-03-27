package com.stavros.graham

import android.Manifest
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

private const val TAG = "ConversationScreen"

@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel = viewModel(),
    onOpenSettings: () -> Unit = {},
) {
    val state by conversationViewModel.state.collectAsState()
    val messages by conversationViewModel.messages.collectAsState()
    val context = LocalContext.current

    var rmsLevel by remember { mutableFloatStateOf(0f) }

    val audioManager = remember { context.getSystemService(AudioManager::class.java) }
    val audioFocusRequest = remember {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .build()
    }

    val speechManager = remember {
        WhisperSpeechRecognizer(
            context = context,
            onResult = { text ->
                rmsLevel = 0f
                conversationViewModel.onSpeechResult(text)
            },
            onError = { error ->
                Log.w(TAG, "Speech error: $error")
                rmsLevel = 0f
                conversationViewModel.onListeningError()
            },
            onRmsChanged = { rms -> rmsLevel = rms },
        )
    }

    val ttsManager = remember { PiperTtsManager(context) }

    DisposableEffect(Unit) {
        onDispose {
            speechManager.destroy()
            ttsManager.destroy()
        }
    }

    // Initialize both the speech recognizer and TTS once on first composition.
    LaunchedEffect(Unit) {
        try {
            speechManager.initialize()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            Log.e(TAG, "Speech recognizer initialization failed", exception)
        }
        try {
            ttsManager.initialize()
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            Log.e(TAG, "TTS initialization failed", exception)
        }
    }

    // React to state transitions: start/stop the recognizer and drive TTS.
    LaunchedEffect(state) {
        when (state) {
            ConversationState.Listening -> {
                audioManager.requestAudioFocus(audioFocusRequest)
                context.startForegroundService(Intent(context, ConversationService::class.java))
                speechManager.startListening()
            }
            ConversationState.Speaking -> {
                speechManager.stopListening()
                val text = messages.lastOrNull { !it.isUser }?.text
                if (text != null) {
                    try {
                        ttsManager.speak(text)
                        conversationViewModel.onSpeakingDone()
                    } catch (exception: Exception) {
                        if (exception is CancellationException) throw exception
                        Log.e(TAG, "TTS speak failed", exception)
                        conversationViewModel.onTtsError(exception.message ?: "Unknown error")
                    }
                } else {
                    Log.w(TAG, "Speaking state entered but no bot message found")
                    conversationViewModel.onSpeakingDone()
                }
            }
            ConversationState.Sending -> speechManager.stopListening()
            ConversationState.Idle -> {
                speechManager.stopListening()
                ttsManager.stop()
                audioManager.abandonAudioFocusRequest(audioFocusRequest)
                context.stopService(Intent(context, ConversationService::class.java))
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            conversationViewModel.startListening()
        } else {
            Log.w(TAG, "RECORD_AUDIO permission denied; not starting conversation")
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = onOpenSettings) {
                Text("Settings")
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        StatusBar(state = state, rmsLevel = rmsLevel)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            if (state == ConversationState.Idle) {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text("Start", style = MaterialTheme.typography.titleMedium)
                }
            } else {
                Button(
                    onClick = { conversationViewModel.stopConversation() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Text("Stop", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(12.dp),
                )
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSecondaryContainer
                },
            )
        }
    }
}

@Composable
private fun StatusBar(state: ConversationState, rmsLevel: Float) {
    val statusText = when (state) {
        ConversationState.Idle -> "Idle"
        ConversationState.Listening -> "Listening..."
        ConversationState.Sending -> "Thinking..."
        ConversationState.Speaking -> "Speaking..."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (state == ConversationState.Listening) {
                WaveformIndicator(rmsLevel = rmsLevel)
            }
        }
    }
}
