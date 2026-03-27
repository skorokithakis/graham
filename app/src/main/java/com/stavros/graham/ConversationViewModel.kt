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
    private val settings = Settings(application)
    private var chatClient = buildChatClient()

    private fun buildChatClient(): ChatClient =
        ChatClient(serverUrl = settings.serverUrl, bodyTemplate = settings.bodyTemplate)

    // Rebuilds the ChatClient after settings change so the new URL and template take effect.
    fun reloadSettings() {
        chatClient.shutdown()
        chatClient = buildChatClient()
    }

    fun startListening() {
        if (_state.value != ConversationState.Idle) return
        _state.value = ConversationState.Listening
    }

    fun onSpeechResult(text: String) {
        if (_state.value != ConversationState.Listening) return
        _messages.value = _messages.value + ChatMessage(id = nextMessageId++, text = text, isUser = true)
        _state.value = ConversationState.Sending
        viewModelScope.launch {
            try {
                val response = chatClient.sendMessage(text)
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
        _messages.value = _messages.value + ChatMessage(id = nextMessageId++, text = text, isUser = false)
        _state.value = ConversationState.Speaking
    }

    fun onSpeakingDone() {
        if (_state.value != ConversationState.Speaking) return
        _state.value = ConversationState.Listening
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
