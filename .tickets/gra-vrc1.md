---
id: gra-vrc1
status: closed
deps: [gra-s3ak]
links: []
created: 2026-03-28T15:30:24Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Play tones on TTS done and speech recognized, gate all tones behind toggle

In ConversationViewModel.kt:
1. Read tonesEnabled from Settings (in constructor and reloadSettings).
2. After speech recognition completes (Listening → Sending transition in onSpeechResult), play TONE_PROP_ACK for 100ms if tonesEnabled.
3. After TTS finishes (in onSpeakingDone, before transitioning to Listening), play TONE_PROP_PROMPT for 100ms if tonesEnabled.
4. Gate the existing disconnect tone in performHangup() behind tonesEnabled too.
Use the same ToneGenerator pattern as performHangup (create, play, coroutine delay, release). Use STREAM_MUSIC or STREAM_VOICE_CALL consistently with the existing tone. Consider extracting a small helper to avoid duplicating the create/play/delay/release boilerplate three times.

## Design

Helper function like playTone(toneType, durationMs) to DRY up the three call sites.

## Acceptance Criteria

Three tones all gated by tonesEnabled. TTS-done and speech-recognized tones are audibly distinct from each other and from disconnect.

