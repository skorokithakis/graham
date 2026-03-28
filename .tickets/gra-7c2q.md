---
id: gra-7c2q
status: closed
deps: []
links: []
created: 2026-03-27T17:25:56Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Check /stop on any line, not whole message

In ConversationViewModel.kt line 66, change trimmedText == "/stop" to trimmedText.lines().any { it.trim() == "/stop" }. That's the only change.

