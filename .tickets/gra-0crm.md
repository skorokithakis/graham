---
id: gra-0crm
status: open
deps: []
links: []
created: 2026-03-29T14:24:15Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Route tones through AudioTrack instead of ToneGenerator

Replace ToneGenerator usage in ConversationViewModel with AudioTrack-based tone synthesis. Generate simple sine wave tones and play them through an AudioTrack configured with the same AudioAttributes as TTS (USAGE_MEDIA / CONTENT_TYPE_SPEECH). This ensures tones route to the same output as TTS, including Bluetooth A2DP. Keep the same tone semantics: a short ack beep on speech received/speaking done, and a distinct hangup tone on /stop.

## Acceptance Criteria

Tones play through Bluetooth when a Bluetooth audio device is connected.

