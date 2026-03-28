---
id: gra-wqgt
status: closed
deps: []
links: []
created: 2026-03-28T14:30:19Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Add clear conversation button to top app bar

Add a clear/delete icon button to the top app bar in ConversationScreen. It calls a new clearMessages() method on ConversationViewModel that resets the message list to empty. The button is only enabled when ConversationState is Idle.

## Acceptance Criteria

Button visible in top app bar. Disabled when not Idle. Clears message list when tapped.
