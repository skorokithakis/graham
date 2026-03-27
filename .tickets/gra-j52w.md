---
id: gra-j52w
status: closed
deps: []
links: []
created: 2026-03-27T15:46:17Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Handle /stop server response

When the server returns '/stop' as the response text, the conversation should stop instead of being spoken. In onBotResponse in ConversationViewModel: if text == "/stop", add a ChatMessage with text 'Conversation was stopped', isUser=false, isItalic=true, play ToneGenerator.TONE_CDMA_CALLDROP_LITE (use STREAM_VOICE_CALL, max volume, ~500ms duration then release), and set state to Idle. Do NOT transition to Speaking. Add an isItalic: Boolean = false field to ChatMessage. In MessageBubble in ConversationScreen, when message.isItalic is true, apply fontStyle = FontStyle.Italic to the Text.

## Acceptance Criteria

1) /stop response does not get spoken via TTS. 2) Chat shows italic 'Conversation was stopped'. 3) Calldrop tone plays. 4) State goes to Idle.

