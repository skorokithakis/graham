# Graham

Graham is an Android voice conversation app that lets you talk to a remote LLM server using entirely on-device AI models for speech processing — no cloud dependency for STT or TTS.

## Features

- **On-device Speech-to-Text** — Whisper Base (int8) via [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx), with Silero VAD for end-of-speech detection
- **On-device Text-to-Speech** — Piper VITS (`en_US-amy-low`) via sherpa-onnx
- **Configurable chat server** — Send transcripts to any HTTP endpoint and speak the response
- **Basic auth support** — Embed credentials directly in the server URL (`http://user:pass@host/path`)
- **Conversation history** — Scrollable chat view with waveform indicator while listening
- **Foreground service** — Retains microphone access when switching apps

## Requirements

- Android 14+ (minSdk 34)
- ~300 MB free storage for the on-device models

## Building

1. **Download the model files** and place them under `app/src/main/assets/`:

   ```
   whisper-base/
     base-encoder.int8.onnx
     base-decoder.int8.onnx
     base-tokens.txt
     silero_vad.onnx
   vits-piper-en_US-amy-low/
     en_US-amy-low.onnx
     tokens.txt
     espeak-ng-data/
   ```

2. **Build the debug APK:**

   ```bash
   ./gradlew assembleDebug
   ```

   The output APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Configuration

On first launch, open the **Settings** screen (top-right button) and configure:

| Setting | Default | Description |
|---------|---------|-------------|
| Server URL | `http://10.0.2.2:3000/chat` | Full URL of the chat endpoint. Supports `http://user:pass@host/path` for Basic auth. |
| Body template | `{"message": "$transcript", "source": "graham", "sender": "stavros"}` | JSON body sent to the server. Use `$transcript` as a placeholder for the user's speech. |

The server must return a JSON object with a `"response"` key:

```json
{"response": "Hello, how can I help you?"}
```

If the response is not valid JSON, the raw response body is used as the reply.

## Usage

1. Tap **Start** and grant the microphone permission.
2. Speak — Silero VAD detects when you stop talking and Whisper transcribes your speech on-device.
3. The transcript is sent to your configured server via HTTP POST.
4. The server's response is synthesised on-device by Piper and played back.
5. Graham loops back to listening automatically.
6. Tap **Stop** to return to the idle state and release the microphone.

## License

[GNU Affero General Public License v3](LICENSE)
