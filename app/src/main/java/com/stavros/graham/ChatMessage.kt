package com.stavros.graham

data class ChatMessage(val id: Int, val text: String, val isUser: Boolean, val isItalic: Boolean = false)
