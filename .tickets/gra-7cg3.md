---
id: gra-7cg3
status: closed
deps: []
links: []
created: 2026-03-28T02:47:21Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Support Bluetooth headset play/pause to start/stop conversation

Add MediaSession support so Bluetooth headset media buttons control the conversation.

Play/pause on the headset should toggle between Idle and Listening (play starts, pause stops).

Likely approach: create a MediaSession in ConversationService (already a foreground service), set a MediaSession.Callback that handles onPlay/onPause, and bridge events to ConversationViewModel (singleton event bus, broadcast, or similar).

Open questions to resolve before implementation:
- Should it behave as a toggle (single button press alternates start/stop) or map play→start, pause→stop separately?
- Should it work with the screen off / app in background?
- Any other headset buttons to support (next/prev), or just play/pause?

