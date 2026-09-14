---
id: rep-hkitb
status: closed
deps: [rep-erwgl]
links: []
created: 2026-09-13T19:04:59Z
type: chore
priority: 2
assignee: Stavros Korokithakis
---
# Extract duplicated asset-copy code shared by PiperTtsManager and SpeechRecognizer

Objective: PiperTtsManager (lines 39-83) and SpeechRecognizer (lines 62-106) hold byte-identical copies of the sentinel-guarded asset-to-filesDir copy: ensureModelOnDisk/ensureModelsOnDisk, copyAssetDir, copyAssetFile. About 45 duplicated lines. Extract one shared helper and delete both copies.

Scope: add a small top-level file (e.g. ModelAssets.kt) exposing a function that takes a Context and an asset directory name, performs the sentinel-guarded copy, and returns the resulting File. Both managers call it.

Non-goals: do not change the copy strategy or the .copy_complete sentinel behaviour. Do not switch to AssetManager-based model loading. Do not touch model selection, TTS, or STT config.

## Design

The two implementations differ only in the log tag and the singular/plural method name. Keep the log messages substantially intact; they are the main signal for diagnosing first-run copies. The stale-directory deletion added by rep-erwgl must survive the extraction, either inside the helper as an optional parameter or left at the PiperTtsManager call site.


## Notes

**2026-09-13T19:17:50Z**

Approved by user. Ready for implementation.
