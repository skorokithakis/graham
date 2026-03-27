---
id: gra-ewbn
status: closed
deps: [gra-ftrw]
links: []
created: 2026-03-27T13:04:14Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Add About and Model status screens

Implement About and Model status Compose screens. About should show app purpose, high-level stack, and version info. Model status should show local readiness checks for Whisper, Piper, and sherpa AAR by verifying expected files and reporting present/missing plus size where available. Keep checks lightweight and local-only. Non-goals: no runtime inference tests or deep diagnostics.

## Acceptance Criteria

About and Model status screens are reachable from drawer and display expected static/local information.

