---
id: gra-0mby
status: closed
deps: [gra-bpca]
links: []
created: 2026-03-27T00:58:00Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Delete SpeechRecognitionManager.kt

Remove SpeechRecognitionManager.kt from the project. It is fully replaced by WhisperSpeechRecognizer. Verify no remaining references to it anywhere in the codebase.

## Acceptance Criteria

File deleted. No compile errors from missing references.

