---
id: gra-sc3c
status: closed
deps: []
links: []
created: 2026-03-27T15:45:01Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Add markdown rendering

Chat messages should be rendered with markdown, both ways


## Notes

**2026-03-27T16:05:28Z**

Implementation: Add com.mikepenz:multiplatform-markdown-renderer dependency. In MessageBubble, for non-user messages replace Text with the markdown renderer. User messages stay as plain Text. Trim whitespace from message text before rendering. Match existing bubble text colors.
