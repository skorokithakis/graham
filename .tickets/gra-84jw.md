---
id: gra-84jw
status: closed
deps: []
links: []
created: 2026-03-28T00:59:02Z
type: task
priority: 2
assignee: Stavros Korokithakis
---
# Replace Whisper with Parakeet TDT-CTC 110M

Replace the Whisper base int8 ASR model with Parakeet TDT-CTC 110M (non-int8, CTC variant). This is the only task — it touches all the related files in one shot.

**What to change:**

1. **download-models.sh**: Replace the Whisper model downloads with Parakeet downloads from https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet_tdt_ctc_110m-en-36000/resolve/main/. Files needed: model.onnx, tokens.txt. Put them in assets dir under 'parakeet-tdt-ctc-110m' instead of 'whisper-base'. Keep the silero_vad.onnx download but put it in the new directory. Remove the old Whisper downloads.

2. **WhisperSpeechRecognizer.kt**: Rename to SpeechRecognizer.kt. Update:
   - Class name: SpeechRecognizer
   - ASSET_DIR = 'parakeet-tdt-ctc-110m'
   - Replace ENCODER_FILE/DECODER_FILE with MODEL_FILE = 'model.onnx'
   - TOKENS_FILE = 'tokens.txt' (unchanged value)
   - In initialize(): replace OfflineWhisperModelConfig block with OfflineNemoEncDecCtcModelConfig. Config is simple: just set model path. Then set modelConfig.nemo = nemoConfig instead of modelConfig.whisper = whisperConfig. Remove featureDim setting (not needed for NeMo CTC).
   - Update imports: remove OfflineWhisperModelConfig, add OfflineNemoEncDecCtcModelConfig
   - TAG should become 'SpeechRecognizer'

3. **ConversationScreen.kt**: Update WhisperSpeechRecognizer references to SpeechRecognizer.

4. **InfoScreens.kt**: Update the model file check list at line 94-109. Change directory from 'whisper-base' to 'parakeet-tdt-ctc-110m'. Change file list to: 'model.onnx', 'tokens.txt', 'silero_vad.onnx'. Update the variable name from whisperStatuses to asrStatuses or similar.

**Non-goals:**
- Don't add runtime model downloading
- Don't add model selection UI
- Don't change VAD config
- Don't change AudioRecord or recording loop logic
- Don't change the sherpa-onnx AAR version

## Acceptance Criteria

App builds. download-models.sh fetches the Parakeet model files. SpeechRecognizer initializes with NeMo CTC config.

