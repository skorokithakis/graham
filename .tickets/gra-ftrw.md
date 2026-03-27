---
id: gra-ftrw
status: closed
deps: [gra-37cv]
links: []
created: 2026-03-27T13:03:51Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Create app shell with top app bar and drawer navigation

Refactor the main Compose layout to use a Material 3 navigation drawer with a top app bar. Add drawer entries for Conversation, Settings, About, and Model status. Keep single-activity architecture and simple in-memory navigation state. Non-goals: no behavior changes to STT/TTS/chat flow.

## Acceptance Criteria

Drawer opens from app bar, navigation destinations render correctly, and conversation flow still works from the Conversation destination.

