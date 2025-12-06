# Agent Handoff: Built-in Wake Word Detection Implementation

## What Was Implemented

This PR replaced the external app dependency chain (Hotword Plugin + Automate/Tasker) with a fully self-contained wake word detection system.

### Architecture Change

**Before:**
```
User → Hotword Plugin → Automate/Tasker → Home Assistant
       (external app)   (external app)
```

**After:**
```
User → Easy HA Wakeword App → Home Assistant
       (self-contained)
```

### Key Components

1. **WakeWordEngine.kt** (`app/src/main/java/com/roos/easywakeword/wakeword/`)
   - `OnnxModelRunner` - Loads and runs ONNX models for:
     - Mel-spectrogram computation (audio → frequency features)
     - Embedding generation (features → neural network embeddings)
     - Wake word prediction (embeddings → detection score)
   - `WakeWordModel` - Manages audio buffering and streaming prediction

2. **WakeWordService.kt** - Foreground service that:
   - Captures audio at 16kHz mono
   - Processes 1280-sample chunks (80ms)
   - Runs wake word prediction on each chunk
   - Launches Home Assistant Assist when score > 0.5 threshold

3. **SetupWizardActivity.kt** - Simplified 3-step wizard:
   - Step 1: Grant microphone permission
   - Step 2: Disable battery optimization
   - Step 3: Start/stop wake word service

### Bundled Assets (`app/src/main/assets/`)

| File | Purpose | Size |
|------|---------|------|
| `melspectrogram.onnx` | Audio to mel-spectrogram conversion | ~1MB |
| `embedding_model.onnx` | Feature embedding extraction | ~1.3MB |
| `hey_mycroft.onnx` | Wake word classifier | ~200KB |

### Dependencies Added

```groovy
implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.16.0'
implementation 'org.apache.commons:commons-math3:3.6.1'
```

### Permissions Required

```xml
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MICROPHONE" />
```

---

## What Needs Testing

### Functional Testing

1. **Wake Word Detection**
   - [ ] Say "Hey Mycroft" at various distances (close, medium, far)
   - [ ] Test in quiet vs noisy environments
   - [ ] Check for false positives with similar-sounding phrases
   - [ ] Verify detection threshold (0.5) is appropriate

2. **Home Assistant Launch**
   - [ ] Verify HA Companion app launches on detection
   - [ ] Test ASSIST intent action
   - [ ] Test deep link fallback (`homeassistant://navigate/assist`)
   - [ ] Test main app launch fallback

3. **Service Reliability**
   - [ ] Service survives app being swiped from recents
   - [ ] Service restarts after device reboot (if configured)
   - [ ] Service keeps running with screen off
   - [ ] Service keeps running on battery

4. **Setup Wizard**
   - [ ] Microphone permission grant/deny flow
   - [ ] Battery optimization settings open correctly
   - [ ] Service start/stop toggles correctly
   - [ ] UI updates when returning from settings

### Performance Testing

1. **Battery Impact**
   - [ ] Measure battery drain with service running for 24h
   - [ ] Compare to baseline (no service)

2. **CPU/Memory Usage**
   - [ ] Monitor CPU usage during idle listening
   - [ ] Monitor memory usage over time (check for leaks)
   - [ ] Check ONNX model loading time

3. **Latency**
   - [ ] Measure time from wake word to HA Assist opening
   - [ ] Target: < 500ms

### Device Compatibility

- [ ] Test on Android 7.0 (API 24) - minimum supported
- [ ] Test on Android 13+ (API 33) - notification permission
- [ ] Test on various manufacturers (Samsung, Xiaomi, etc.)

### Edge Cases

1. **Audio Conflicts**
   - [ ] Test when another app is using microphone
   - [ ] Test during phone calls
   - [ ] Test with Bluetooth audio connected

2. **Error Handling**
   - [ ] Test with ONNX models missing
   - [ ] Test with permission revoked while running
   - [ ] Test with HA Companion not installed

---

## Known Limitations

1. **Wake Word Model** - Currently using "hey_nugget" model renamed to "hey_mycroft". **Say "Hey Nugget"** to trigger detection (not "Hey Mycroft"). A proper "Hey Mycroft" trained model would be more accurate.

2. **No Model Customization** - Users cannot choose different wake words without bundling new models.

3. **Detection Threshold** - Set to 0.05 based on reference implementations (Re-MENTIA/openwakeword-android-kt). This may need further tuning based on environment.

4. **No Audio Feedback** - No sound/vibration when wake word detected.

---

## Files Changed

| File | Change |
|------|--------|
| `app/build.gradle` | Added ONNX Runtime + commons-math3 dependencies |
| `app/src/main/AndroidManifest.xml` | Added permissions + foreground service |
| `app/src/main/java/.../SetupWizardActivity.kt` | Simplified 3-step wizard |
| `app/src/main/java/.../wakeword/WakeWordEngine.kt` | NEW: ONNX model runner |
| `app/src/main/java/.../wakeword/WakeWordService.kt` | NEW: Background detection service |
| `app/src/main/res/values/strings.xml` | Updated UI strings |
| `app/src/main/assets/*.onnx` | NEW: Bundled models |
| `README.md` | Updated documentation |
| `THIRD_PARTY_NOTICES.md` | NEW: License attributions |

---

## Licensing

Code derived from Apache 2.0 licensed projects:
- [OpenWakeWord](https://github.com/dscripka/openWakeWord) by David Scripka
- [OpenWakeWord for Android](https://github.com/hasanatlodhi/OpenwakewordforAndroid) by Hasanat Ahmed Lodhi

See `THIRD_PARTY_NOTICES.md` for full attribution.
