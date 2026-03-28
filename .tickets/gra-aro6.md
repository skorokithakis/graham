---
id: gra-aro6
status: closed
deps: [gra-s3ak]
links: []
created: 2026-03-28T15:30:13Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Add tones toggle to SettingsScreen

Add a Switch to SettingsScreen.kt for the tonesEnabled setting. Follow the same pattern as other settings: local mutableStateOf initialized from settings.tonesEnabled, written on Save. Label it 'Enable tones'.

## Acceptance Criteria

SettingsScreen shows a labeled Switch that persists tonesEnabled on save.

