---
id: gra-s3ak
status: closed
deps: []
links: []
created: 2026-03-28T15:30:08Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Add tonesEnabled setting

Add a boolean 'tonesEnabled' property to Settings.kt (default true). Key: KEY_TONES_ENABLED. Follow the exact pattern of the existing ttsSpeed property but with getBoolean/putBoolean.

## Acceptance Criteria

Settings.kt has a tonesEnabled var backed by SharedPreferences, defaulting to true.

