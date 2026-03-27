---
id: gra-bpca
status: closed
deps: [gra-tjlo]
links: []
created: 2026-03-27T00:56:41Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Update ConversationScreen to use WhisperSpeechRecognizer

Replace SpeechRecognitionManager with WhisperSpeechRecognizer in ConversationScreen.kt.

Changes:
- Replace the remember { SpeechRecognitionManager(...) } block with remember { WhisperSpeechRecognizer(...) }. The callbacks are similar but onError now takes a String, not an Int.
- Add a LaunchedEffect(Unit) to call whisperRecognizer.initialize() (like the existing TTS init block). Can be combined or kept separate.
- Remove the RETRYABLE_ERRORS set and the Handler-based retry logic in onError. On error, just log and call conversationViewModel.onListeningError().
- Remove the import of android.speech.SpeechRecognizer and related imports.
- The LaunchedEffect(state) block stays mostly the same — startListening/stopListening/destroy have the same names.
- startListening() is now a regular function (not suspend), so no change needed in the LaunchedEffect call site.

Non-goals: no ViewModel changes. No UI changes.

## Acceptance Criteria

ConversationScreen no longer references android.speech.* classes. App builds with ./gradlew assembleDebug.

