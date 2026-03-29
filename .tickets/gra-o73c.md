---
id: gra-o73c
status: open
deps: []
links: []
created: 2026-03-29T14:24:21Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Enable Bluetooth SCO for microphone input

Add Bluetooth SCO management so the app uses the Bluetooth headset microphone for STT when one is connected. Use AudioManager.startBluetoothSco() when entering Listening state and stopBluetoothSco() when leaving it. Register a BroadcastReceiver for ACTION_SCO_AUDIO_STATE_UPDATED to track SCO connection state. Set AudioRecord's audio source to VOICE_COMMUNICATION when SCO is active, DEFAULT otherwise. Add BLUETOOTH_CONNECT permission (runtime permission on API 31+).

## Acceptance Criteria

STT captures audio from the Bluetooth headset microphone when one is connected.

