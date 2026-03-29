---
id: gra-aa3f
status: closed
deps: []
links: []
created: 2026-03-29T15:13:21Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Wipe audio logs on app start

Delete all files in cacheDir/audio_logs/ (both tts/ and stt/ subdirectories) when the app starts. Do this early in MainActivity or Application.onCreate. Simple recursive delete of the directory contents, then recreate the empty directories.

