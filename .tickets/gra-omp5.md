---
id: gra-omp5
status: closed
deps: []
links: []
created: 2026-03-27T17:27:19Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Handle /stop on any line in bot response

In ConversationViewModel.kt onBotResponse: instead of checking trimmedText == "/stop", check if any line (trimmed) equals "/stop". If found: remove that line from the text, if remaining text is non-blank add it as a normal bot ChatMessage, then add the italic 'Conversation was stopped' message, play the calldrop tone, set state to Idle. If no /stop line found, behave as before (existing path).

