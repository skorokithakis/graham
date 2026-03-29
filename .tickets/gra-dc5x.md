---
id: gra-dc5x
status: closed
deps: []
links: []
created: 2026-03-29T14:24:09Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Store TTS and STT audio to temp storage

Save all TTS output (synthesized FloatArray) and STT input (recorded PCM samples) to WAV files in the app's cache directory. TTS: after synthesis completes, write the samples to a timestamped WAV file before/during playback. STT: accumulate the audio samples that were sent to the recognizer and write them to a timestamped WAV file after recognition completes. Files go in cacheDir/audio_logs/tts/ and cacheDir/audio_logs/stt/. Keep it simple — just write WAV files, no cleanup policy yet.

## Acceptance Criteria

WAV files appear in the cache directory after TTS playback and STT recognition. Files are playable.

