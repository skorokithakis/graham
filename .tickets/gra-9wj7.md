---
id: gra-9wj7
status: closed
deps: []
links: []
created: 2026-03-27T13:52:29Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Add persisted TTS speed setting

Add a new settings value for TTS speed in Settings.kt with a sensible default matching current behavior (1.3). Persist as a float in shared preferences and expose getter/setter. Non-goals: no other settings changes.

## Acceptance Criteria

TTS speed setting is persisted and can be read by UI and TTS manager.

