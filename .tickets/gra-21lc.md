---
id: gra-21lc
status: closed
deps: [gra-ftrw]
links: []
created: 2026-03-27T13:04:21Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Integrate existing Settings screen into drawer navigation

Remove ad-hoc settings toggle state from MainActivity and route Settings through the drawer destination model. Preserve existing SettingsScreen behavior and save flow (reload settings in ConversationViewModel). Non-goals: no settings schema changes.

