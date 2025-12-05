# Terms of Reference (ToR)
## Project
Self-contained Android app for always-on wake word detection to trigger Home Assistant Assist, using built-in OpenWakeWord neural network processing.

## Goal
Deliver a minimal-tap setup on old Android phones/tablets: grant microphone permission, disable battery optimization, and start the wake word detection service. The app listens for "Hey Mycroft" and opens Home Assistant Assist with no external app dependencies and no typing required.

## Scope
- Build a self-contained Kotlin Android app (single-activity wizard + foreground service).
- Integrate OpenWakeWord via ONNX Runtime for on-device wake word detection.
- Bundle ONNX models for mel-spectrogram, audio embeddings, and wake word classification.
- Keep everything on-device; no cloud services, no external apps, no user typing required.

## Wizard Steps (UX)
1) Grant microphone permission (required for wake word detection).
2) Disable battery optimization (ensures service stays running).
3) Start wake word detection service (one tap to begin listening).

## Wake Word Detection
- **Audio Capture**: 16kHz mono, 1280-sample chunks (80ms).
- **Processing Pipeline**: Audio → Mel-spectrogram → Embeddings → Wake word classifier.
- **Models**: ONNX format, bundled in assets (~2.5MB total).
- **Threshold**: Detection score > 0.5 triggers action.
- **Action**: Launch HA Assist via intent; fallback to deep link or main HA app.

## Foreground Service
- Persistent notification shows "Listening for wake word".
- Continuous audio capture and real-time processing.
- Survives app being swiped from recents.
- Works with screen off and on battery.

## Permissions and Reliability
- RECORD_AUDIO: Required for wake word detection.
- FOREGROUND_SERVICE + FOREGROUND_SERVICE_MICROPHONE: Required for background listening.
- POST_NOTIFICATIONS: Required on Android 13+ for service notification.
- REQUEST_IGNORE_BATTERY_OPTIMIZATIONS: Prompts user to exempt app from battery optimization.

## Deliverables
- Self-contained Android app source + build instructions.
- Bundled ONNX models (melspectrogram.onnx, embedding_model.onnx, hey_mycroft.onnx).
- README with setup steps and architecture documentation.
- THIRD_PARTY_NOTICES.md with license attributions for OpenWakeWord.
- AGENT_HANDOFF.md with testing/debugging guidance.

## Dependencies
- `com.microsoft.onnxruntime:onnxruntime-android:1.16.0` - Neural network inference.
- `org.apache.commons:commons-math3:3.6.1` - Audio processing utilities.

## Acceptance Criteria
- On a clean device, with no typing: install app → grant mic permission → disable battery optimization → start service → say "Hey Mycroft" → Home Assistant Assist opens.
- No external apps required (Hotword Plugin, Automate, Tasker not needed).
- All processing happens on-device with no internet required for detection.
