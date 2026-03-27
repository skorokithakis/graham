---
id: gra-p1uc
status: closed
deps: [gra-9wj7]
links: []
created: 2026-03-27T13:52:40Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Add TTS speed slider in settings and wire into Piper

Add a slider control to SettingsScreen for TTS speed and display current value. Use a practical range (0.8 to 1.8) and persist via Settings. Update PiperTtsManager to read configured speed instead of hardcoded value. Keep existing TTS flow and APIs unchanged otherwise. Non-goals: no voice/model selection and no other UI redesign.

## Acceptance Criteria

Changing slider updates saved speed and speaking uses the configured value.

