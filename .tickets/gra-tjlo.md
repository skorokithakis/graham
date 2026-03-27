---
id: gra-tjlo
status: closed
deps: [gra-bw8b]
links: []
created: 2026-03-27T00:56:30Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Implement WhisperSpeechRecognizer to replace SpeechRecognitionManager

Create WhisperSpeechRecognizer.kt that replaces SpeechRecognitionManager. It uses AudioRecord (16kHz mono 16-bit) + sherpa-onnx Silero VAD + sherpa-onnx OfflineRecognizer with Whisper base int8.

Key design:
- Constructor takes Context plus callbacks: onResult: (String) -> Unit, onError: (String) -> Unit, onRmsChanged: (Float) -> Unit.
- suspend fun initialize(): copies model files from assets/whisper-base/ to filesDir/whisper-base/ using the same sentinel-file pattern as PiperTtsManager (reuse or extract that helper). Creates the Vad instance (SileroVadModelConfig with model path, sampleRate=16000) and OfflineRecognizer (OfflineWhisperModelConfig with encoder/decoder/tokens paths, language=en, task=transcribe; OfflineModelConfig with tokens path, numThreads=2, provider=cpu).
- fun startListening(): starts AudioRecord, launches a coroutine that reads short[] chunks (sized to VAD windowSize, typically 512 samples at 16kHz), converts to float[], feeds to vad.acceptWaveform(), computes RMS and calls onRmsChanged. When vad.isSpeechDetected() was true and becomes false, check !vad.empty(), pop the SpeechSegment, create an OfflineStream, acceptWaveform the segment samples, decode, get result text, call onResult. Then reset VAD state for next utterance. If the segment text is blank, keep listening.
- fun stopListening(): stops AudioRecord, resets VAD.
- fun destroy(): releases AudioRecord, Vad, and OfflineRecognizer.

The coroutine loop should run on Dispatchers.IO. startListening/stopListening are called from main thread.

Use @Volatile flags to coordinate start/stop. Do NOT use Android SpeechRecognizer at all.

Non-goals: no changes to ConversationScreen or ViewModel yet. No partial results.

## Design

VAD windowSize must match what Silero expects (512 for 16kHz). AudioRecord buffer should be at least a few windows. The reading loop converts Short PCM to Float by dividing by Short.MAX_VALUE. RMS is computed from the float samples per chunk. The onError callback takes a String message (not an int error code like the old manager).

## Acceptance Criteria

Class compiles. Uses sherpa-onnx Vad, OfflineRecognizer, OfflineWhisperModelConfig. Follows existing code conventions (no DI, coroutines, Log.d/w/e for logging).

