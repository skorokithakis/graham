---
id: gra-9bjz
status: closed
deps: []
links: []
created: 2026-03-27T13:40:14Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Set custom launcher icon

Add app launcher icon resources based on the existing Graham logo and wire them in AndroidManifest.xml. Create adaptive icon resources (ic_launcher and ic_launcher_round) with a simple neutral background and the logo foreground. Since minSdk is 34, adaptive icon resources are sufficient. Update <application> to set android:icon and android:roundIcon. Non-goals: no changes to in-app top bar logo or app behavior.

## Acceptance Criteria

Launcher icon appears as Graham logo on device home/app drawer after install.

