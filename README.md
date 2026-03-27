# Graham

A voice conversation Android app. Graham records your speech, transcribes it
locally with Whisper, sends the text to a configurable HTTP endpoint, and speaks
the response back using Piper TTS. Everything runs on-device except the chat
backend.

## How it works

1. Press **Start** to begin a conversation.
2. Speak naturally. Silero VAD detects when you stop talking.
3. Your speech is transcribed by Whisper (base, int8 quantized) via sherpa-onnx.
4. The transcript is POSTed to your configured server URL.
5. The server's response is spoken back with Piper TTS.
6. The app listens again automatically for your next turn.
7. Press **Stop** to end the conversation.

The app pauses other media when a conversation starts and resumes it when you
stop. A foreground service keeps the microphone active if you switch to another
app.

## Setup

### Model files

Model files are not included in the repository. Run the download script before
building:

```sh
./download-models.sh
```

This downloads Whisper base (int8), Silero VAD, Piper TTS, and the sherpa-onnx
AAR.

### Configuration

In the app, open the drawer and tap **Settings** to configure:

- **Server URL**: The HTTP endpoint that receives transcripts.
- **Body template**: The JSON body sent to the server. Use `$transcript` as a
  placeholder for the transcribed text.

The server should return a JSON object with a `response` field, or plain text.

### Building

```sh
./gradlew assembleDebug
./gradlew installDebug
```

## Stack

- Kotlin, Jetpack Compose, Material 3
- sherpa-onnx for both STT (Whisper) and TTS (Piper)
- Silero VAD for speech boundary detection
- OkHttp for the chat backend
