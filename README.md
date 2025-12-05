# Easy Android HA Wakeword App

A minimal-tap Android helper app for setting up always-on wake word detection to trigger Home Assistant Assist.

## Overview

This app provides a simple wizard to configure:
- **Hotword Plugin** - For always-on wake word detection
- **Automate** - For automation flow handling
- **Home Assistant** - Opens Assist when wake word is detected

No typing required - just follow the prompts!

## Prerequisites

- Android 7.0+ (API level 24+)
- [Home Assistant Companion App](https://play.google.com/store/apps/details?id=io.homeassistant.companion.android) installed and configured

## Setup Steps (Tap-by-Tap Sequence)

### Step 1: Install Hotword Plugin
Tap "Install Hotword Plugin" to open Play Store and install [Hotword Plugin](https://play.google.com/store/apps/details?id=com.pocketsphinx.hotword).

### Step 2: Install Automate
Tap "Install Automate" to open Play Store and install [Automate](https://play.google.com/store/apps/details?id=com.llamalab.automate).

### Step 3: Import Automate Flow
Tap "Import Flow" to import the bundled wake word flow into Automate. This flow:
- Listens for wake word detection from Hotword Plugin
- Opens Home Assistant Assist when triggered
- Runs as a foreground service for reliability

### Step 4: Enable the Flow
Tap "Open Automate" and toggle on the "HA Wake Word" flow.

### Step 5: Load Wake Word Model
Tap "Import Wake Word Model" to load the "Hey Mycroft" model into Hotword Plugin. Then enable it in Hotword Plugin settings.

### Step 6: Disable Battery Optimization
For reliable always-on detection, disable battery optimization for:
- Hotword Plugin
- Automate
- Home Assistant Companion

## Home Assistant Intent/Deeplink Constants

| Constant | Value |
|----------|-------|
| HA Package | `io.homeassistant.companion.android` |
| Assist Action | `android.intent.action.ASSIST` |
| Main Activity | `io.homeassistant.companion.android.launch.LaunchActivity` |
| Assist Deeplink | `homeassistant://assist` |

### Example Intent for Automate

```
Action: android.intent.action.ASSIST
Package: io.homeassistant.companion.android
```

### Alternative: Direct Activity Launch

```
Action: android.intent.action.MAIN
Package: io.homeassistant.companion.android
Class: io.homeassistant.companion.android.launch.LaunchActivity
```

### Webhook Fallback (for advanced users)

If the HA app isn't responding to intents, you can trigger via webhook:
```
POST https://your-ha-instance/api/webhook/{webhook_id}
```

## Automate Flow Behavior

The bundled `.flo` file implements:

1. **Trigger**: Broadcast receiver for `com.pocketsphinx.hotword.DETECTED`
2. **Action**: Launch Home Assistant Assist via intent
3. **Fallback**: Open HA main activity if Assist intent fails
4. **Settings**:
   - Foreground service notification (stays alive)
   - No screen/headset/charging gating (always active)
   - Optional webhook block (disabled by default)

## Wake Word Models

The app bundles a placeholder for the "Hey Mycroft" model. For production use, download the actual model from:
- [OpenWakeWord Project](https://github.com/dscripka/openWakeWord)
- [Mycroft Precise](https://github.com/MycroftAI/mycroft-precise)

Supported wake words (via OpenWakeWord):
- `hey_mycroft`
- `ok_nabu`
- `alexa`
- `hey_jarvis`
- `hey_rhasspy`

## Building

### Requirements
- Android Studio Arctic Fox or later
- JDK 17
- Android SDK 34

### Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

### Output
APK files are generated in `app/build/outputs/apk/`

## Permissions

| Permission | Purpose |
|------------|---------|
| `POST_NOTIFICATIONS` | Show status notifications (Android 13+) |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prompt for battery exemption |

Note: Microphone permission is requested by Hotword Plugin, not this app.

## Troubleshooting

### Wake word not detecting
1. Ensure Hotword Plugin is running (check notification)
2. Verify the wake word model is loaded and enabled
3. Check battery optimization is disabled

### Home Assistant not opening
1. Verify HA Companion app is installed
2. Check Automate flow is enabled
3. Try launching HA manually first

### Flow stops working
1. Disable battery optimization for Automate
2. Enable "Start on boot" in Automate settings
3. Check for system "app hibernation" settings

## Related Projects

- [Wyoming Satellite on Termux](https://github.com/pantherale0/wyoming-satellite-termux) - Alternative approach using Termux terminal
- [OpenWakeWord](https://github.com/dscripka/openWakeWord) - Wake word models
- [Home Assistant Companion](https://companion.home-assistant.io/) - HA Android app

## License

MIT License - See LICENSE file for details.
