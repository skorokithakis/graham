# Architecture

## Detected stack

- **Language:** Kotlin 2.0.21 (100% of source; `gradle/libs.versions.toml`)
- **Platform:** Android — minSdk 24, targetSdk 35, compileSdk 35 (`app/build.gradle.kts`)
- **UI framework:** Jetpack Compose + Material 3 (`compose-bom = "2024.12.01"`, `app/build.gradle.kts`)
- **Build system:** Gradle 8 with Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`); version catalog at `gradle/libs.versions.toml`
- **AGP version:** 8.7.3

### Dependencies (from `gradle/libs.versions.toml` + `app/build.gradle.kts`)

| Library | Version | Purpose |
|---|---|---|
| `androidx.core:core-ktx` | 1.15.0 | Android KTX extensions |
| `androidx.activity:activity-compose` | 1.9.3 | Compose activity integration |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | ViewModel in Compose |
| `androidx.compose:compose-bom` | 2024.12.01 | Compose BOM |
| `androidx.compose.material3:material3` | (BOM-managed) | Material 3 UI |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP client for chat server |
| `com.mikepenz:multiplatform-markdown-renderer-m3` | 0.27.0 | Renders Markdown in bot message bubbles |
| `sherpa-onnx-1.12.32.aar` | 1.12.32 (local) | On-device STT + TTS via ONNX runtime (k2-fsa/sherpa-onnx) |

### Speech / audio

- **STT:** On-device Parakeet TDT-CTC 110M via sherpa-onnx `OfflineRecognizer` (`OfflineNemoEncDecCtcModelConfig`), with Silero VAD for end-of-speech detection. Audio is captured at 16 kHz mono 16-bit via `AudioRecord`. Silero VAD processes 512-sample windows (`VAD_WINDOW_SIZE = 512`). Model files live in `app/src/main/assets/parakeet-tdt-ctc-110m/`. (`SpeechRecognizer.kt`)
- **TTS:** sherpa-onnx v1.12.32 (local `.aar`) running the **Piper VITS** model `en_US-amy-low.onnx` entirely on-device. Synthesis produces a `FloatArray` which is converted to PCM 16-bit `ShortArray` and streamed via `AudioTrack` (`USAGE_MEDIA` / `CONTENT_TYPE_SPEECH`). A Bluetooth A2DP tail-wait polls `playbackHeadPosition` before calling `stop()` to avoid cutting off buffered audio on wireless headsets. Speed is user-configurable (0.8×–1.8×, default 1.3×). Model files live in `app/src/main/assets/vits-piper-en_US-amy-low/`. (`PiperTtsManager.kt`)
- **Tones:** Android `ToneGenerator` on `STREAM_VOICE_CALL` at max volume. `TONE_PROP_ACK` (200 ms) plays when speech is received and when speaking finishes; `TONE_CDMA_CALLDROP_LITE` (500 ms) plays on `/stop` hangup. User-toggleable via `Settings.tonesEnabled`. (`ConversationViewModel.kt`)
- **Audio focus:** `AUDIOFOCUS_GAIN_TRANSIENT` with `USAGE_VOICE_COMMUNICATION` is requested when listening starts and abandoned on idle, pausing other media. (`ConversationScreen.kt`)
- **Markdown stripping:** Bot responses are rendered with Markdown in the UI but stripped of syntax before TTS via `stripMarkdown()` so formatting characters are not read aloud. (`MarkdownUtils.kt`)

### Deployment / runtime

- Single-module Android app (`include(":app")` in `settings.gradle.kts`).
- No Docker, CI, or cloud tooling detected.
- No signing config in `app/build.gradle.kts` (release build has `isMinifyEnabled = false` and no explicit `signingConfig`).

---

## What "dialog" / "conversation" means in this codebase

There is no Android `Dialog` widget anywhere in this app. The word "dialog" does not appear in the source. The concept the codebase uses instead is a **conversation**: a back-and-forth voice exchange between the user and a remote chat server, mediated by on-device STT and TTS.

The conversation is modelled as a four-state machine (`ConversationState.kt`):

```
Idle → Listening → Sending → Speaking → Listening → …
```

- **Idle** — nothing is happening; the "Start" button is shown.
- **Listening** — `AudioRecord` is capturing mic input; Silero VAD watches for speech boundaries; when speech ends, Parakeet TDT-CTC 110M transcribes it.
- **Sending** — the transcript has been handed to `ChatClient`, which POSTs it to the configured server URL and awaits a response.
- **Speaking** — the server's response text is being synthesised and played back by `PiperTtsManager`; when playback finishes the state returns to `Listening` automatically.

The `ConversationViewModel` owns the state machine and the message list (`List<ChatMessage>`). `ConversationScreen` observes both via `StateFlow` and drives `SpeechRecognizer` and `PiperTtsManager` in response to state changes via `LaunchedEffect(state)`.

---

## How server communication works

**Class:** `ChatClient` (`app/src/main/java/com/stavros/graham/ChatClient.kt`)

**Transport:** OkHttp 4 — a single `OkHttpClient` instance per `ChatClient`, with 120-second timeouts on connect, read, and write.

**Request shape:**
- Method: `POST`
- Content-Type: `application/json`
- URL: user-configurable at runtime (stored in `SharedPreferences` via `Settings.kt`; default `http://10.0.2.2:3000/chat`)
- Body: a user-configurable JSON template (also in `SharedPreferences`; default `{"message": "$transcript", "source": "graham", "sender": "stavros"}`). The literal string `$transcript` in the template is replaced with the JSON-escaped transcription before sending.

**Authentication:** If the URL contains `user:pass@host` credentials, `ChatClient.parseBasicAuth()` strips them from the URL and adds an `Authorization: Basic …` header. No other auth mechanism exists.

**Response shape:** The server should return a JSON object with a `"response"` string field. If JSON parsing fails, the raw response body is used as-is. A non-2xx HTTP status throws `IOException`.

**Lifecycle:** `ConversationViewModel` constructs a `ChatClient` from the current settings on startup and on every `reloadSettings()` call (triggered when the user saves settings). `ChatClient.shutdown()` drains the OkHttp dispatcher and connection pool.

**Call site:** `ConversationViewModel.onSpeechResult()` calls `chatClient.sendMessage(text)` inside a `viewModelScope.launch` coroutine. The call runs on `Dispatchers.IO` (enforced inside `ChatClient.sendMessage` via `withContext`).

---

## Conventions

- **Formatting/linting:** No linter or formatter config found (no `.editorconfig`, no `detekt`, no `ktlint`). `gradle.properties` sets `kotlin.code.style=official`. No pre-commit hooks.
- **Type checking:** Kotlin's own compiler; no additional static analysis tool configured.
- **Testing:** No test sources or test dependencies found (no `test/` or `androidTest/` directories, no JUnit/Espresso in the version catalog).
- **Documentation:** `README.md` at repo root; `ARCHITECTURE.md` (this file); no changelog.

---

## Linting and testing commands

No linter, formatter, or test runner is configured. The only available commands are:

```sh
./gradlew assembleDebug   # build a debug APK
./gradlew installDebug    # build and install on a connected device
```

(from `gradlew` at repo root, documented in `README.md`)

---

## Project structure hotspots

```
app/build.gradle.kts              — all dependencies and Android config
gradle/libs.versions.toml         — version catalog (single source of truth for versions)
app/src/main/AndroidManifest.xml  — permissions (RECORD_AUDIO, INTERNET, FOREGROUND_SERVICE_MICROPHONE) and service declaration
app/src/main/java/com/stavros/graham/
  MainActivity.kt                 — entry point; Compose host; ModalNavigationDrawer routing across 4 destinations
  ConversationViewModel.kt        — owns the state machine (Idle→Listening→Sending→Speaking) and message list
  ConversationScreen.kt           — main UI; drives SpeechRecognizer and PiperTtsManager via LaunchedEffect(state)
  ConversationState.kt            — enum: Idle, Listening, Sending, Speaking
  ChatMessage.kt                  — data class: id, text, isUser, isItalic
  ChatClient.kt                   — OkHttp POST to configurable server; basic-auth parsing; JSON body templating
  Settings.kt                     — SharedPreferences wrapper: serverUrl, bodyTemplate, ttsSpeed, tonesEnabled
  SpeechRecognizer.kt             — sherpa-onnx Parakeet TDT-CTC 110M + Silero VAD; AudioRecord loop; asset-to-disk copy
  PiperTtsManager.kt              — sherpa-onnx Piper VITS; AudioTrack streaming playback; asset-to-disk copy
  ConversationService.kt          — foreground service (FOREGROUND_SERVICE_TYPE_MICROPHONE) to keep mic alive in background
  SettingsScreen.kt               — settings UI: server URL, body template, TTS speed slider, tones toggle
  InfoScreens.kt                  — AboutScreen and ModelStatusScreen (filesystem checks for model files)
  WaveformIndicator.kt            — animated bar waveform driven by RMS level during Listening state
app/src/main/assets/vits-piper-en_US-amy-low/
                                  — bundled Piper TTS model + espeak-ng phoneme data (large; not in git, downloaded by script)
app/libs/sherpa-onnx-1.12.32.aar  — local ONNX runtime AAR (not from Maven; downloaded by download-models.sh)
download-models.sh                — downloads all model files and the sherpa-onnx AAR before first build
```

---

## Do and don't patterns

### Do

- **State machine via `StateFlow`.** All conversation state is a single `MutableStateFlow<ConversationState>` in `ConversationViewModel`; the UI and audio subsystems react to it via `collectAsState()` and `LaunchedEffect(state)`. (`ConversationViewModel.kt`, `ConversationScreen.kt`)
- **Coroutines for all async work.** TTS synthesis and HTTP calls run on `Dispatchers.Default` / `Dispatchers.IO` via `withContext`; recording runs on a `SupervisorJob`-backed `CoroutineScope(Dispatchers.IO)`. (`PiperTtsManager.kt`, `ChatClient.kt`, `SpeechRecognizer.kt`)
- **`CancellationException` re-throw.** Every `catch (exception: Exception)` block that is not specifically scoped to a non-coroutine context explicitly re-throws `CancellationException`. (`ConversationViewModel.kt:46`, `ConversationScreen.kt:104,131`)
- **Sentinel file for asset copy.** A `.copy_complete` marker prevents silently using a partially-copied model directory after a crash; the directory is deleted and re-copied if the sentinel is absent. (`PiperTtsManager.kt:ensureModelOnDisk`, `SpeechRecognizer.kt:ensureModelsOnDisk`)
- **Basic auth parsed from URL.** `ChatClient.parseBasicAuth()` strips `user:pass@` from the server URL and converts it to an `Authorization: Basic …` header rather than storing credentials separately. (`ChatClient.kt:70-84`)
- **`SupervisorJob` for the recording scope.** A failed recording coroutine does not cancel the enclosing scope, so a subsequent `startListening()` call can launch a new coroutine. (`SpeechRecognizer.kt:48`)
- **`currentCoroutineContext().ensureActive()` in the audio write loop.** Playback can be cancelled cooperatively mid-stream without waiting for the full buffer to drain. (`PiperTtsManager.kt:181`)

### Don't

- **No broad exception swallowing.** The one `catch (_: Exception)` in `ChatClient` is scoped only to JSON parsing fallback (returns raw body instead of crashing). (`ChatClient.kt:49`)
- **No platform STT.** STT does not use Android's `SpeechRecognizer` service; all recognition runs on-device via sherpa-onnx. (no `android.speech` import anywhere)
- **No dependency injection framework.** Objects are constructed directly; no Hilt, Koin, or Dagger.
- **No navigation library.** Screen routing is a `var currentDestination: NavigationDestination` state variable in `MainActivity`, switched by a `ModalNavigationDrawer`. (`MainActivity.kt:58,119-131`)
- **No Android `Dialog` widgets.** There are no `AlertDialog`, `BottomSheetDialog`, or similar modal overlays anywhere in the codebase.

---

## Open questions

1. **No tests exist.** It is unclear whether tests are planned or intentionally omitted. Any new logic has no test harness to validate against.
2. **Release signing.** `app/build.gradle.kts` has no `signingConfig` for the release build type. How release APKs/AABs are signed is not captured in the repo.
3. **Chat server protocol.** The server URL and JSON body template are user-configurable at runtime. The expected server-side API shape (beyond returning `{"response": "..."}` or plain text) is not documented in the repo. The default template sends `{"message": "...", "source": "graham", "sender": "stavros"}`.
4. **Conversation history.** `ChatClient.sendMessage()` sends only the current transcript — no prior messages. Whether the server maintains session state is unknown and not handled client-side.
