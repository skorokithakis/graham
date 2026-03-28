package com.stavros.graham

import android.Manifest
import com.stavros.graham.stripMarkdown
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException

private const val TAG = "ConversationScreen"

@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel = viewModel(),
) {
    val state by conversationViewModel.state.collectAsState()
    val messages by conversationViewModel.messages.collectAsState()
    val context = LocalContext.current

    var rmsLevel by remember { mutableFloatStateOf(0f) }
    var speechReady by remember { mutableStateOf(false) }

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
        SpeechRecognizer(
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
            speechManager.stopListening()
            ttsManager.stop()
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
            context.stopService(Intent(context, ConversationService::class.java))
            speechManager.destroy()
            ttsManager.destroy()
        }
    }

    // Initialize both the speech recognizer and TTS once on first composition.
    LaunchedEffect(Unit) {
        try {
            speechManager.initialize()
            speechReady = true
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
    // speechReady is included as a key so this re-runs once initialization completes,
    // and the early return prevents startListening() from being called before the
    // recognizer is ready.
    LaunchedEffect(state, speechReady) {
        if (!speechReady) return@LaunchedEffect
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
                        ttsManager.speak(stripMarkdown(text))
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

    // The Scaffold in MainActivity already applies innerPadding (top bar + nav bar) to
    // the wrapping Surface, so no additional system-bar inset handling is needed here.
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message)
            }
        }

        HorizontalDivider()
        StatusBar(state = state, rmsLevel = rmsLevel)
        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                .widthIn(max = 300.dp)
                .background(
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            if (!message.isUser && !message.isItalic) {
                Markdown(
                    content = message.text,
                    colors = markdownColor(text = MaterialTheme.colorScheme.onSecondaryContainer),
                )
            } else {
                Text(
                    text = message.text,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    fontStyle = if (message.isItalic) FontStyle.Italic else null,
                )
            }
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
                .padding(horizontal = 16.dp, vertical = 10.dp),
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
