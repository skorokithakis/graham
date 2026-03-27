---
id: gra-1ifm
status: closed
deps: []
links: []
created: 2026-03-27T15:45:26Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Screen should turn off normally if not listening

The screen timeout should be the normal timeout if the app is idle


## Notes

**2026-03-27T15:59:37Z**

Implementation: Remove unconditional FLAG_KEEP_SCREEN_ON from onCreate. Observe conversationViewModel.state in Compose content and toggle the flag — set it when state is not Idle, clear it when Idle. All work in MainActivity.kt.
