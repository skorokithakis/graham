---
id: gra-q3ix
status: closed
deps: []
links: []
created: 2026-03-27T18:11:02Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Speak bot message before hanging up on /stop

When /stop is found in the bot response with remaining text, the text should be spoken before hanging up. Add a private var pendingStop = false to ConversationViewModel. In onBotResponse's /stop branch: if remainingText is non-blank, add it as a bot ChatMessage, set pendingStop = true, go to Speaking state (don't add the italic message or play the tone yet). If remainingText is blank, keep the current immediate hangup behavior. In onSpeakingDone: if pendingStop is true, reset it to false, add the italic 'Conversation was stopped' ChatMessage, play the calldrop tone, set state to Idle (same hangup code currently in onBotResponse). If pendingStop is false, go to Listening as before.

