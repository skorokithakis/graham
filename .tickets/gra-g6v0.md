---
id: gra-g6v0
status: closed
deps: [gra-dc5x]
links: []
created: 2026-03-29T15:50:08Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Add audio replay buttons to message bubbles

Add a small speaker/play icon button to each message bubble in ConversationScreen. Tapping it plays the corresponding WAV file from cacheDir/audio_logs/. TTS messages play from audio_logs/tts/, STT messages play from audio_logs/stt/. Button is disabled (or hidden) when conversation state is not Idle. Requires associating each ChatMessage with its WAV file path — store the path on ChatMessage when the audio is saved. Use a simple AudioTrack or MediaPlayer for playback, matching USAGE_MEDIA / CONTENT_TYPE_SPEECH attributes.

## Design

ChatMessage needs a new nullable audioFilePath field. PiperTtsManager and SpeechRecognizer need to return/expose the file path after saving. The ViewModel wires the path onto the ChatMessage. ConversationScreen reads the path and plays it on tap. Only one replay can play at a time — tapping another stops the current one.

## Acceptance Criteria

Each message bubble with a saved audio file shows a play button. Tapping it plays the audio. Button is disabled when conversation is not Idle.

