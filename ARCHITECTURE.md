# Architecture

## Detected stack

- **Language:** Kotlin (100% of source; `gradle/libs.versions.toml` pins `kotlin = "2.0.21"`)
- **Platform:** Android (minSdk 34, targetSdk 35, compileSdk 35 — `app/build.gradle.kts`)
- **UI framework:** Jetpack Compose + Material3 (`compose-bom = "2024.12.01"`, `app/build.gradle.kts`)
- **Build system:** Gradle 8 with Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`); version catalog at `gradle/libs.versions.toml`
- **AGP version:** 8.7.3

### Dependencies (from `gradle/libs.versions.toml` + `app/build.gradle.kts`)

| Library | Version | Purpose |
|---|---|---|
| `androidx.core:core-ktx` | 1.15.0 | Android KTX extensions |
| `androidx.activity:activity-compose` | 1.9.3 | Compose activity integration |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | ViewModel in Compose |
| `androidx.compose:compose-bom` | 2024.12.01 | Compose BOM |
| `androidx.compose.material3:material3` | (BOM-managed) | Material3 UI |
| `com.squareup.okhttp3:okhttp` | 4.12.0 | HTTP client for chat server |
| `sherpa-onnx-1.12.32.aar` | 1.12.32 (local) | On-device TTS via ONNX runtime (k2-fsa/sherpa-onnx) |

### Speech / audio

- **STT:** On-device Whisper base int8 via sherpa-onnx `OfflineRecognizer`, with Silero VAD for end-of-speech detection. Audio is captured at 16 kHz mono 16-bit via `AudioRecord`. (`WhisperSpeechRecognizer.kt`)
- **TTS:** [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) v1.12.32 (local `.aar`) running the **Piper VITS** model `en_US-amy-low.onnx` entirely on-device. Model files live in `app/src/main/assets/vits-piper-en_US-amy-low/`. (`PiperTtsManager.kt`)

### Deployment / runtime

- Single-module Android app (`include(":app")` in `settings.gradle.kts`).
- No Docker, CI, or cloud tooling detected.
- No signing config in `app/build.gradle.kts` (release build has `isMinifyEnabled = false` and no explicit signingConfig).

---

## Conventions

- **Formatting/linting:** No linter or formatter config found (no `.editorconfig`, no `detekt`, no `ktlint`). `gradle.properties` sets `kotlin.code.style=official`.
- **Type checking:** Kotlin's own compiler; no additional static analysis tool configured.
- **Testing:** No test sources or test dependencies found (no `test/` or `androidTest/` directories, no JUnit/Espresso in the version catalog).
- **Documentation:** No docs folder, no changelog, no README.

---

## Linting and testing commands

No linter, formatter, or test runner is configured. The only available command is:

```
./gradlew assembleDebug
```

(from `gradlew` at repo root)

---

## Project structure hotspots

```
app/build.gradle.kts              — all dependencies and Android config
gradle/libs.versions.toml         — version catalog (single source of truth for versions)
app/src/main/java/com/stavros/graham/
  MainActivity.kt                 — entry point; Compose host, screen routing
  ConversationViewModel.kt        — central state machine (Idle→Listening→Sending→Speaking)
  ConversationScreen.kt           — main UI
  WhisperSpeechRecognizer.kt      — wraps sherpa-onnx Whisper + Silero VAD; STT
  PiperTtsManager.kt              — wraps sherpa-onnx VITS; on-device TTS
  ChatClient.kt                   — OkHttp POST to configurable chat server
  Settings.kt                     — SharedPreferences wrapper (server URL, body template)
  ConversationState.kt            — enum: Idle, Listening, Sending, Speaking
  ChatMessage.kt                  — data class for conversation messages
  SettingsScreen.kt               — settings UI
app/src/main/assets/vits-piper-en_US-amy-low/
                                  — bundled Piper TTS model + espeak-ng phoneme data (~large)
app/libs/sherpa-onnx-1.12.32.aar  — local ONNX runtime AAR (not from Maven)
```

---

## Do and don't patterns

### Do

- **Fresh recognizer instance per session.** `WhisperSpeechRecognizer` creates a new `OfflineRecognizer` and `AudioRecord` for each `startListening()` call and tears them down on stop/error, avoiding state leakage between sessions. (`WhisperSpeechRecognizer.kt`)
- **Coroutines for async work.** TTS synthesis and HTTP calls run on `Dispatchers.Default` / `Dispatchers.IO` via `withContext`; UI state is `StateFlow`. (`PiperTtsManager.kt`, `ChatClient.kt`, `ConversationViewModel.kt`)
- **Sentinel file for asset copy.** A `.copy_complete` marker prevents silently using a partially-copied model directory after a crash. (`PiperTtsManager.kt:ensureModelOnDisk`)
- **`CancellationException` re-throw.** The `catch (exception: Exception)` block in `ConversationViewModel` explicitly re-throws `CancellationException` to preserve coroutine cancellation semantics. (`ConversationViewModel.kt:42-46`)
- **Basic auth parsed from URL.** `ChatClient` strips `user:pass@` from the server URL and converts it to an `Authorization: Basic …` header rather than storing credentials separately. (`ChatClient.kt:parseBasicAuth`)

### Don't

- **No broad exception swallowing.** The one `catch (_: Exception)` in `ChatClient` is scoped only to JSON parsing fallback (returns raw body instead of crashing). (`ChatClient.kt:44`)
- **No platform STT delegation.** STT does not use Android's `SpeechRecognizer` service; all recognition runs on-device via sherpa-onnx.
- **No dependency injection framework.** Objects are constructed directly; no Hilt, Koin, or Dagger.
- **No navigation library.** Screen routing is a simple `var showSettings: Boolean` state variable in `MainActivity`. (`MainActivity.kt`)

---

## Open questions

1. **No tests exist.** It is unclear whether tests are planned or intentionally omitted. Any new logic has no test harness to validate against.
2. **Release signing.** `app/build.gradle.kts` has no `signingConfig` for the release build type. How release APKs/AABs are signed is not captured in the repo.
3. **Chat server protocol.** The server URL and JSON body template are user-configurable at runtime. The expected server-side API shape (beyond returning `{"response": "..."}`) is not documented in the repo.
