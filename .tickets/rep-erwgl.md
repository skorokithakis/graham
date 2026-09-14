---
id: rep-erwgl
status: closed
deps: []
links: []
created: 2026-09-13T19:04:51Z
type: task
priority: 1
assignee: Stavros Korokithakis
---
# Switch Piper TTS voice from amy-low to amy-medium

Objective: replace the bundled Piper voice en_US-amy-low (16 kHz, low tier) with en_US-amy-medium (22.05 kHz, medium tier). Same speaker, same VITS family, same file layout, so no synthesis or playback logic changes.

Scope:
- download-models.sh: point the Piper block at https://huggingface.co/csukuangfj/vits-piper-en_US-amy-medium (files: en_US-amy-medium.onnx, en_US-amy-medium.onnx.json, tokens.txt, MODEL_CARD, plus the espeak-ng-data/ tree via the existing python block). Target dir app/src/main/assets/vits-piper-en_US-amy-medium.
- PiperTtsManager.kt: ASSET_DIR and MODEL_FILE constants.
- PiperTtsManager.ensureModelOnDisk(): also delete a stale filesDir/vits-piper-en_US-amy-low directory if present, so the old 63 MB copy is not orphaned on devices that ran the previous build. Add a comment that this can be dropped in a later release.
- InfoScreens.kt: model status directory and file names, plus the About screen stack line naming the voice.
- .gitignore: replace the amy-low assets path with the amy-medium one.
- README.md and ARCHITECTURE.md: both name amy-low.

Non-goals: no voice picker in Settings. No change to the TTS speed setting, playback, or the Bluetooth A2DP tail-wait logic. No STT changes. Do not rename the PiperTtsManager class.

## Design

Output sample rate rises from 16 kHz to 22.05 kHz. playAudio() already reads the rate from GeneratedAudio at runtime and sizes the AudioTrack buffer from it, so there is no constant to update. Worth a listen on a Bluetooth output anyway, because the buffer size and therefore the tail-wait timing shift.

## Acceptance Criteria

App builds and speaks with the new voice. Model status screen reports the amy-medium files as present. No stale vits-piper-en_US-amy-low directory remains in filesDir after launch.


## Notes

**2026-09-13T19:17:50Z**

Approved by user. Ready for implementation.
