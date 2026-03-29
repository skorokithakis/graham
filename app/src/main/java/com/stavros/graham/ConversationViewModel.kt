package com.stavros.graham

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ConversationViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(ConversationState.Idle)
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var nextMessageId = 0
    private var pendingStop = false
    private val settings = Settings(application)
    private var chatClient = buildChatClient()
    private var tonesEnabled = settings.tonesEnabled

    private fun buildChatClient(): ChatClient =
        ChatClient(serverUrl = settings.serverUrl, bodyTemplate = settings.bodyTemplate)

    // Rebuilds the ChatClient after settings change so the new URL and template take effect.
    fun reloadSettings() {
        chatClient.shutdown()
        chatClient = buildChatClient()
        tonesEnabled = settings.tonesEnabled
    }

    fun startListening() {
        if (_state.value != ConversationState.Idle) return
        _state.value = ConversationState.Listening
    }

    fun onSpeechResult(text: String, audioFilePath: String?) {
        if (_state.value != ConversationState.Listening) return
        val trimmedText = text.trim()
        _messages.value = _messages.value + ChatMessage(
            id = nextMessageId++,
            text = trimmedText,
            isUser = true,
            audioFilePath = audioFilePath,
        )
        _state.value = ConversationState.Sending
        viewModelScope.launch {
            if (tonesEnabled) TonePlayer.playAckTone()
        }
        viewModelScope.launch {
            try {
                val response = chatClient.sendMessage(trimmedText)
                onBotResponse(response)
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                _messages.value = _messages.value + ChatMessage(
                    id = nextMessageId++,
                    text = "Error: ${exception.message ?: "Network error"}",
                    isUser = false,
                )
                if (_state.value == ConversationState.Sending) {
                    _state.value = ConversationState.Listening
                }
            }
        }
    }

    fun onBotResponse(text: String) {
        if (_state.value != ConversationState.Sending) return
        val trimmedText = text.trim()
        if (trimmedText.lines().any { it.trim() == "/stop" }) {
            val remainingText = trimmedText.lines()
                .filter { it.trim() != "/stop" }
                .joinToString("\n")
                .trim()
            if (remainingText.isNotBlank()) {
                _messages.value = _messages.value + ChatMessage(id = nextMessageId++, text = remainingText, isUser = false)
                pendingStop = true
                _state.value = ConversationState.Speaking
            } else {
                performHangup()
            }
            return
        }
        _messages.value = _messages.value + ChatMessage(id = nextMessageId++, text = trimmedText, isUser = false)
        _state.value = ConversationState.Speaking
    }

    fun attachTtsAudioPath(audioFilePath: String) {
        val current = _messages.value
        val lastBotIndex = current.indexOfLast { !it.isUser }
        if (lastBotIndex == -1) return
        _messages.value = current.toMutableList().also { list ->
            list[lastBotIndex] = list[lastBotIndex].copy(audioFilePath = audioFilePath)
        }
    }

    fun onSpeakingDone() {
        if (_state.value != ConversationState.Speaking) return
        if (pendingStop) {
            pendingStop = false
            performHangup()
        } else {
            if (tonesEnabled) viewModelScope.launch { TonePlayer.playAckTone() }
            _state.value = ConversationState.Listening
        }
    }

    private fun performHangup() {
        _messages.value = _messages.value + ChatMessage(
            id = nextMessageId++,
            text = "Conversation was stopped",
            isUser = false,
            isItalic = true,
        )
        if (tonesEnabled) viewModelScope.launch { TonePlayer.playHangupTone() }
        _state.value = ConversationState.Idle
    }

    fun clearMessages() {
        _messages.value = emptyList()
    }

    fun stopConversation() {
        _state.value = ConversationState.Idle
    }

    fun onListeningError() {
        if (_state.value != ConversationState.Listening) return
        _state.value = ConversationState.Idle
    }

    fun onTtsError(message: String) {
        if (_state.value != ConversationState.Speaking) return
        _messages.value = _messages.value + ChatMessage(
            id = nextMessageId++,
            text = "Error speaking response: $message",
            isUser = false,
        )
        _state.value = ConversationState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        chatClient.shutdown()
    }
}
