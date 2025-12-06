# Easy Android HA Wakeword App

[![Android](https://img.shields.io/badge/Android-7.0%2B-green.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Home Assistant](https://img.shields.io/badge/Home%20Assistant-Compatible-41BDF5.svg)](https://www.home-assistant.io/)

> **Transform any old Android phone or tablet into an always-listening wake word device for Home Assistant Assist — with zero typing required.**

<p align="center">
  <img src="docs/wizard-preview.png" alt="Setup Wizard Preview" width="300"/>
</p>

---

## 📥 Download

<p align="center">
  <a href="https://github.com/R00S/easy-android-ha-wakeword-app/releases/latest/download/easy-wakeword.apk">
    <img src="https://img.shields.io/badge/Download-APK-brightgreen?style=for-the-badge&logo=android" alt="Download APK"/>
  </a>
</p>

**[⬇️ Download Latest APK](https://github.com/R00S/easy-android-ha-wakeword-app/releases/latest/download/easy-wakeword.apk)** | **[All Releases](https://github.com/R00S/easy-android-ha-wakeword-app/releases)**

> 💡 You may need to enable "Install from unknown sources" in your Android settings to install the APK.

### First Release Setup

To create the first release with the APK:
1. Go to **Actions** tab → **Build and Release APK** → **Run workflow**
2. Or push a version tag: `git tag v1.0.0 && git push origin v1.0.0`

The workflow will automatically build and attach the APK to the release.

---

## 🎯 What This Does

This app provides a **simple 3-step wizard** that configures your Android device to:

1. **Listen** for the wake word "Hey Mycroft" (always-on, using built-in wake word detection)
2. **Trigger** Home Assistant Assist when the wake word is detected
3. **Stay running** reliably in the background with no user intervention

**No external apps required, no typing, no configuration files** — just tap through the wizard and you're done!

---

## 📋 Table of Contents

- [Download](#-download)
- [Features](#-features)
- [Requirements](#-requirements)
- [Quick Start](#-quick-start)
- [Detailed Setup Guide](#-detailed-setup-guide)
- [How It Works](#-how-it-works)
- [Building From Source](#-building-from-source)
- [Troubleshooting](#-troubleshooting)
- [For Developers](#-for-developers)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🎙️ **Always-On Listening** | Wake word detection runs continuously in the background |
| 🏠 **Home Assistant Integration** | Opens HA Assist directly when triggered |
| 📱 **One-Tap Setup** | 3-step wizard guides you through setup |
| 🔋 **Battery Optimized** | Efficient on-device neural network processing |
| 🔒 **On-Device Processing** | All wake word detection happens locally — no cloud required |
| ⚡ **No External Apps** | Everything runs within this single app |

---

## 📱 Requirements

### Hardware
- Android phone or tablet (great for repurposing old devices!)
- Built-in or external microphone

### Software
- **Android 7.0** (Nougat) or later — API level 24+
- [Home Assistant Companion App](https://play.google.com/store/apps/details?id=io.homeassistant.companion.android) — installed and signed in
- Internet connection to Home Assistant (local network is fine)

---

## 🚀 Quick Start

1. **[Download the APK](https://github.com/R00S/easy-android-ha-wakeword-app/releases/latest/download/easy-wakeword.apk)** and install it on your Android device
2. **Open** the app and follow the 3-step wizard
3. **Say** "Hey Mycroft" — Home Assistant Assist will open!

That's it! The wizard handles all the configuration automatically.

---

## 📖 Detailed Setup Guide

### Step 1: Grant Microphone Permission

Tap **"Grant Microphone Permission"** to allow the app to listen for wake words.

**Why it's needed:** The app needs microphone access to detect when you say "Hey Mycroft". All audio processing happens locally on your device — no audio is sent to any server.

---

### Step 2: Disable Battery Optimization

Tap **"Disable Battery Optimization"** to ensure the wake word detection runs reliably.

**Why it's needed:** Android may stop background services to save battery. Disabling optimization for this app ensures it keeps running.

---

### Step 3: Start Wake Word Detection

Tap **"Start Wake Word Detection"** to begin listening.

**What happens:**
- A foreground service starts with a persistent notification
- The app continuously listens for "Hey Mycroft"
- When detected, Home Assistant Assist opens automatically

---

## ⚙️ How It Works

```
┌─────────────────────────────────────────────────────────────────┐
│                        Your Android Device                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │              Easy HA Wakeword App                        │   │
│   │                                                          │   │
│   │   ┌─────────────────┐                                   │   │
│   │   │  Microphone     │ ──── Captures audio               │   │
│   │   └────────┬────────┘                                   │   │
│   │            │                                             │   │
│   │            ▼                                             │   │
│   │   ┌─────────────────┐                                   │   │
│   │   │  OpenWakeWord   │ ──── Neural network detection     │   │
│   │   │  (ONNX Runtime) │      "Hey Mycroft" → 0.85 score   │   │
│   │   └────────┬────────┘                                   │   │
│   │            │                                             │   │
│   │            │ Score > threshold?                         │   │
│   │            ▼                                             │   │
│   │   ┌─────────────────┐                                   │   │
│   │   │  Launch Intent  │ ──── Opens Home Assistant Assist  │   │
│   │   └─────────────────┘                                   │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                   │
│   ┌─────────────────┐                                            │
│   │ Home Assistant  │ ──── Voice assistant ready                 │
│   │   Companion     │                                            │
│   └─────────────────┘                                            │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### Built-in Wake Word Detection

The app uses [OpenWakeWord](https://github.com/dscripka/openWakeWord) for on-device wake word detection:

- **ONNX Runtime** for efficient neural network inference
- **Mel-spectrogram processing** for audio feature extraction
- **Embedding model** for audio representation
- **Wake word classifier** for "Hey Mycroft" detection

All processing happens locally — no internet required for detection!

---

## 🔨 Building From Source

### Prerequisites

| Requirement | Version |
|-------------|---------|
| Android Studio | Arctic Fox (2020.3.1) or later |
| JDK | 17 |
| Android SDK | 34 (compileSdk) |
| Kotlin | 1.9.10 |

### Clone and Build

```bash
# Clone the repository
git clone https://github.com/R00S/easy-android-ha-wakeword-app.git
cd easy-android-ha-wakeword-app

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install directly to connected device
./gradlew installDebug
```

### Output

APK files are generated in:
```
app/build/outputs/apk/debug/app-debug.apk
app/build/outputs/apk/release/app-release-unsigned.apk
```

### Project Structure

```
easy-android-ha-wakeword-app/
├── app/
│   ├── src/main/
│   │   ├── java/com/roos/easywakeword/
│   │   │   ├── SetupWizardActivity.kt    # Main wizard UI
│   │   │   └── wakeword/
│   │   │       ├── WakeWordEngine.kt     # ONNX model runner
│   │   │       └── WakeWordService.kt    # Background service
│   │   ├── assets/
│   │   │   ├── embedding_model.onnx      # Audio embedding model
│   │   │   ├── melspectrogram.onnx       # Mel-spectrogram model
│   │   │   └── hey_mycroft.onnx          # Wake word model
│   │   ├── res/
│   │   │   ├── layout/                   # XML layouts
│   │   │   ├── values/                   # Strings, colors, themes
│   │   │   └── drawable/                 # Icons and graphics
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
├── THIRD_PARTY_NOTICES.md                # Third-party licenses
└── README.md
```

---

## 🔍 Troubleshooting

### Wake Word Not Detecting

| Issue | Solution |
|-------|----------|
| Service not running | Check for persistent notification; restart via wizard |
| Microphone permission denied | Grant mic permission in app settings |
| Battery optimization killing service | Disable battery optimization for this app |
| Threshold too high | Wake word requires clear pronunciation |

### Home Assistant Not Opening

| Issue | Solution |
|-------|----------|
| HA Companion not installed | Install from Play Store |
| Not signed in to HA | Open HA app and complete sign-in |
| Wrong app version | Ensure you have the latest HA Companion |

### Service Stops After Reboot

1. **Disable battery optimization** for this app
2. **Check for "App hibernation"** in Android settings (Android 12+)
3. **Lock app in recent apps** (swipe down to lock)

### Device-Specific Issues

| Device/Manufacturer | Common Issue | Solution |
|---------------------|--------------|----------|
| Samsung | Aggressive battery optimization | Disable in Device Care |
| Xiaomi/MIUI | Autostart blocked | Enable autostart in Security app |
| Huawei/EMUI | App killed in background | Add to "Protected apps" |
| OnePlus | Battery optimization | Disable in Battery settings |

---

## 👨‍💻 For Developers

### App Permissions

| Permission | Purpose | Required |
|------------|---------|----------|
| `RECORD_AUDIO` | Wake word detection | Yes |
| `FOREGROUND_SERVICE` | Background listening | Yes |
| `FOREGROUND_SERVICE_MICROPHONE` | Microphone in foreground service | Yes |
| `POST_NOTIFICATIONS` | Show status notifications | Android 13+ only |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prompt for battery exemption | Optional |

### Wake Word Models

The app bundles the **"Hey Mycroft"** wake word model from [OpenWakeWord](https://github.com/dscripka/openWakeWord).

**Model details:**
- Wake word: "Hey Mycroft"
- Detection threshold: 0.05
- Processing: Real-time (80ms chunks)
- Framework: ONNX Runtime

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit issues and pull requests.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

### Third-Party Licenses

This project uses code from the following open-source projects (Apache 2.0 licensed):

- [OpenWakeWord](https://github.com/dscripka/openWakeWord) by David Scripka
- [OpenWakeWord for Android](https://github.com/hasanatlodhi/OpenwakewordforAndroid) by Hasanat Ahmed Lodhi

See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for full license details.

---

## 🙏 Acknowledgments

- [Home Assistant](https://www.home-assistant.io/) — The amazing home automation platform
- [OpenWakeWord](https://github.com/dscripka/openWakeWord) — Open source wake word detection
- [OpenWakeWord for Android](https://github.com/hasanatlodhi/OpenwakewordforAndroid) — Android port inspiration
- [ONNX Runtime](https://onnxruntime.ai/) — Efficient neural network inference

---

<p align="center">
  Made with ❤️ for the Home Assistant community
</p>
