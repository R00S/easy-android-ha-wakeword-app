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

This helper app provides a **5-step wizard** that configures your Android device to:

1. **Listen** for the wake word "Hey Mycroft" (always-on, using minimal battery)
2. **Trigger** Home Assistant Assist when the wake word is detected
3. **Stay running** reliably in the background with no user intervention

**No typing, no configuration files, no command line** — just tap through the wizard and you're done!

---

## 📋 Table of Contents

- [Download](#-download)
- [Features](#-features)
- [Requirements](#-requirements)
- [Quick Start](#-quick-start)
- [Detailed Setup Guide](#-detailed-setup-guide)
- [How It Works](#-how-it-works)
- [Building From Source](#-building-from-source)
- [Configuration](#-configuration)
- [Troubleshooting](#-troubleshooting)
- [For Developers](#-for-developers)
- [Alternative Approaches](#-alternative-approaches)
- [Contributing](#-contributing)
- [License](#-license)

---

## ✨ Features

| Feature | Description |
|---------|-------------|
| 🎙️ **Always-On Listening** | Wake word detection runs continuously in the background |
| 🏠 **Home Assistant Integration** | Opens HA Assist directly when triggered |
| 📱 **One-Tap Setup** | Wizard guides you through every step |
| 🔋 **Battery Optimized** | Uses efficient wake word detection via Hotword Plugin |
| 🔒 **On-Device Processing** | All wake word detection happens locally — no cloud required |
| ⚡ **No Typing Required** | Perfect for wall-mounted tablets or dedicated devices |

---

## 📱 Requirements

### Hardware
- Android phone or tablet (great for repurposing old devices!)
- Built-in or external microphone

### Software
- **Android 7.0** (Nougat) or later — API level 24+
- [Home Assistant Companion App](https://play.google.com/store/apps/details?id=io.homeassistant.companion.android) — installed and signed in
- Internet connection to Home Assistant (local network is fine)

### Third-Party Apps (installed via wizard)
- [Hotword Plugin](https://play.google.com/store/apps/details?id=com.pocketsphinx.hotword) — for wake word detection
- [Automate](https://play.google.com/store/apps/details?id=com.llamalab.automate) — for automation flow handling

---

## 🚀 Quick Start

1. **[Download the APK](https://github.com/R00S/easy-android-ha-wakeword-app/releases/latest/download/easy-wakeword.apk)** and install it on your Android device
2. **Open** the app and follow the 5-step wizard
3. **Say** "Hey Mycroft" — Home Assistant Assist will open!

That's it! The wizard handles all the configuration automatically.

---

## 📖 Detailed Setup Guide

### Step 1: Install Hotword Plugin

<img align="right" src="docs/step1.png" alt="Step 1" width="200"/>

Tap **"Install Hotword Plugin"** to open the Play Store.

**What it does:** Hotword Plugin provides efficient, always-on wake word detection. It runs in the background and listens for your configured wake word.

> 💡 **Tip:** After installing, grant microphone permissions when prompted.

<br clear="right"/>

---

### Step 2: Install Automate

<img align="right" src="docs/step2.png" alt="Step 2" width="200"/>

Tap **"Install Automate"** to open the Play Store.

**What it does:** Automate is a powerful automation app that responds to wake word detections and launches Home Assistant Assist.

<br clear="right"/>

---

### Step 3: Import the Automate Flow

<img align="right" src="docs/step3.png" alt="Step 3" width="200"/>

Tap **"Import Flow"** to import the pre-configured automation.

**What happens:**
- The bundled `.flo` file is shared to Automate
- Automate opens with the import dialog
- Tap **"Import"** in Automate to confirm

<br clear="right"/>

---

### Step 4: Enable the Flow

<img align="right" src="docs/step4.png" alt="Step 4" width="200"/>

Tap **"Open Automate"** and enable the "HA Wake Word" flow.

**In Automate:**
1. Find "HA Wake Word" in your flows list
2. Tap the flow to open it
3. Tap the **▶️ Start** button to enable it

> ⚠️ **Important:** Enable "Run on system startup" in Automate settings so it survives reboots.

<br clear="right"/>

---

### Step 5: Load the Wake Word Model

<img align="right" src="docs/step5.png" alt="Step 5" width="200"/>

Tap **"Import Wake Word Model"** to load "Hey Mycroft" into Hotword Plugin.

**In Hotword Plugin:**
1. Accept the imported model
2. Enable the "Hey Mycroft" wake word
3. Start the listening service

<br clear="right"/>

---

### Step 6: Disable Battery Optimization

For reliable always-on operation, disable battery optimization for:

| App | Why |
|-----|-----|
| **Hotword Plugin** | Keeps wake word detection running |
| **Automate** | Keeps automation flows active |
| **Home Assistant** | Ensures quick response |

**How to disable:**
1. Go to **Settings → Apps → [App Name] → Battery**
2. Select **"Don't optimize"** or **"Unrestricted"**

---

## ⚙️ How It Works

```
┌─────────────────────────────────────────────────────────────────┐
│                        Your Android Device                       │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│   ┌─────────────────┐                                            │
│   │  Hotword Plugin │ ──── Listens for "Hey Mycroft"             │
│   └────────┬────────┘                                            │
│            │                                                      │
│            │ Broadcasts: com.pocketsphinx.hotword.DETECTED       │
│            ▼                                                      │
│   ┌─────────────────┐                                            │
│   │    Automate     │ ──── Receives broadcast, runs flow         │
│   └────────┬────────┘                                            │
│            │                                                      │
│            │ Intent: android.intent.action.ASSIST                │
│            ▼                                                      │
│   ┌─────────────────┐                                            │
│   │ Home Assistant  │ ──── Opens Assist screen                   │
│   │   Companion     │                                            │
│   └─────────────────┘                                            │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

### The Automate Flow

The bundled `.flo` file contains a simple but reliable flow:

1. **Trigger:** Broadcast Receive block listening for `com.pocketsphinx.hotword.DETECTED`
2. **Action:** Activity Start block launching Home Assistant Assist
3. **Loop:** Returns to listening after each activation

**Flow Settings:**
- ✅ Foreground service (persistent notification)
- ✅ No screen state gating (works with screen off)
- ✅ No charging state gating (works on battery)
- ❌ No headset gating (always listens)

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
│   │   │   └── SetupWizardActivity.kt    # Main wizard UI
│   │   ├── assets/
│   │   │   ├── ha_wakeword.flo           # Automate flow
│   │   │   └── hey_mycroft.tflite        # Wake word model
│   │   ├── res/
│   │   │   ├── layout/                   # XML layouts
│   │   │   ├── values/                   # Strings, colors, themes
│   │   │   └── drawable/                 # Icons and graphics
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
├── settings.gradle
└── README.md
```

---

## 🔧 Configuration

### Home Assistant Intent Constants

Use these values if you're customizing the Automate flow or building your own integration:

| Constant | Value |
|----------|-------|
| **Package** | `io.homeassistant.companion.android` |
| **Assist Action** | `android.intent.action.ASSIST` |
| **Main Activity** | `io.homeassistant.companion.android.launch.LaunchActivity` |
| **Assist Deeplink** | `homeassistant://assist` |

### Automate Intent Block Settings

```yaml
Action: android.intent.action.ASSIST
Package: io.homeassistant.companion.android
```

### Alternative: Direct Activity Launch

```yaml
Action: android.intent.action.MAIN
Package: io.homeassistant.companion.android
Class: io.homeassistant.companion.android.launch.LaunchActivity
```

### Webhook Fallback (Advanced)

If the HA app doesn't respond to intents, use a webhook:

```yaml
Method: POST
URL: https://your-ha-instance/api/webhook/{webhook_id}
```

Configure the webhook in Home Assistant's `configuration.yaml`:
```yaml
automation:
  - alias: "Wake Word Webhook"
    trigger:
      - platform: webhook
        webhook_id: your_webhook_id
    action:
      - service: assist_pipeline.run
        data:
          start_stage: stt
```

---

## 🔍 Troubleshooting

### Wake Word Not Detecting

| Issue | Solution |
|-------|----------|
| Hotword Plugin not running | Check for persistent notification; restart the service |
| Model not loaded | Re-import the wake word model in Hotword Plugin |
| Microphone permission denied | Grant mic permission in app settings |
| Battery optimization killing service | Disable battery optimization for Hotword Plugin |

### Home Assistant Not Opening

| Issue | Solution |
|-------|----------|
| HA Companion not installed | Install from Play Store |
| Not signed in to HA | Open HA app and complete sign-in |
| Automate flow not running | Enable the "HA Wake Word" flow in Automate |
| Wrong intent configuration | Verify intent settings match documentation |

### Automation Stops After Reboot

1. **Enable "Run on system startup"** in Automate settings
2. **Disable battery optimization** for Automate
3. **Check for "App hibernation"** in Android settings (Android 12+)
4. **Lock Automate in recent apps** (swipe down to lock)

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
| `POST_NOTIFICATIONS` | Show status notifications | Android 13+ only |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prompt for battery exemption | Optional |

> **Note:** Microphone permission is requested by Hotword Plugin, not this app.

### Wake Word Models

The app bundles the **"Hey Mycroft"** wake word model from [OpenWakeWord](https://github.com/dscripka/openWakeWord) — ready to use out of the box with no additional downloads required.

**Model details:**
- Wake word: "Hey Mycroft"
- Size: ~840 KB
- License: Creative Commons Attribution-NonCommercial-ShareAlike 4.0
- Source: [OpenWakeWord v0.5.1](https://github.com/dscripka/openWakeWord/releases/tag/v0.5.1)

**Other available wake words (via OpenWakeWord):**
- `ok_nabu` (Home Assistant native)
- `alexa`
- `hey_jarvis`
- `hey_rhasspy`

### Hotword Plugin Broadcast

When a wake word is detected, Hotword Plugin sends:

```java
Intent intent = new Intent("com.pocketsphinx.hotword.DETECTED");
intent.putExtra("keyword", "hey mycroft");
sendBroadcast(intent);
```

---

## 🔄 Alternative Approaches

If this approach doesn't suit your needs, consider these alternatives:

| Approach | Pros | Cons |
|----------|------|------|
| **[Wyoming Satellite on Termux](https://github.com/pantherale0/wyoming-satellite-termux)** | Full Wyoming protocol, more wake words | Requires Termux, command-line setup |
| **[Home Assistant Assist](https://www.home-assistant.io/voice_control/)** | Native integration | Requires button press or Google/Alexa |
| **[ESPHome Voice Assistant](https://esphome.io/components/voice_assistant.html)** | Dedicated hardware | Requires ESP32 device |

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

---

## 🙏 Acknowledgments

- [Home Assistant](https://www.home-assistant.io/) — The amazing home automation platform
- [Hotword Plugin](https://play.google.com/store/apps/details?id=com.pocketsphinx.hotword) — Efficient wake word detection
- [Automate](https://llamalab.com/automate/) — Powerful Android automation
- [OpenWakeWord](https://github.com/dscripka/openWakeWord) — Open source wake word models
- [Wyoming Satellite on Termux](https://github.com/pantherale0/wyoming-satellite-termux) — Inspiration and reference

---

<p align="center">
  Made with ❤️ for the Home Assistant community
</p>
