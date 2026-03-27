---
id: gra-bw8b
status: closed
deps: []
links: []
created: 2026-03-27T00:56:10Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Download and bundle Whisper base int8 and Silero VAD model files into assets

Download the following files and place them in app/src/main/assets/whisper-base/: base-encoder.int8.onnx, base-decoder.int8.onnx, base-tokens.txt from https://huggingface.co/csukuangfj/sherpa-onnx-whisper-base/resolve/main/ and silero_vad.onnx from https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/silero_vad.onnx. Non-goals: no code changes in this task, just the asset files.

