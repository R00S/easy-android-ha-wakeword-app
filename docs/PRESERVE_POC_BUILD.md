# Preserving the Proof of Concept Build

This document explains how to preserve the current working build (Android 13/14) as a proof of concept release.

## Steps to Create the Preserved Release

### Option 1: Using GitHub Web Interface

1. Go to https://github.com/R00S/easy-android-ha-wakeword-app/releases
2. Click "Draft a new release"
3. Set the tag to `v1.0.0-poc`
4. Set the release title to "v1.0.0 - Proof of Concept"
5. Download the current working APK from the "latest" release:
   - https://github.com/R00S/easy-android-ha-wakeword-app/releases/download/latest/easy-wakeword.apk
6. Upload this APK to the new release as `easy-wakeword.apk`
7. Add release notes:
   ```markdown
   ## Proof of Concept Build
   
   This is a preserved build for public testing and demonstration purposes.
   
   ### ✅ Confirmed Working
   - Android 13 (API 33)
   - Android 14 (API 34)
   
   ### ⚠️ Known Issues
   - Crashes on Android 10 (API 29) - Fix in development
   
   ### Features
   - Built-in wake word detection using OpenWakeWord
   - "Hey Mycroft" wake word support
   - Launches Home Assistant Assist when triggered
   - Simple 3-step setup wizard
   - No external apps required
   
   This build is preserved as reference while Android 10 compatibility fixes are being developed.
   ```
8. Mark as "Set as a pre-release" if desired
9. Click "Publish release"

### Option 2: Using GitHub CLI

```bash
# Download the current working APK
wget https://github.com/R00S/easy-android-ha-wakeword-app/releases/download/latest/easy-wakeword.apk

# Create the preserved release
gh release create v1.0.0-poc \
  --title "v1.0.0 - Proof of Concept" \
  --notes "Proof of Concept build - Confirmed working on Android 13/14. Known issue on Android 10." \
  --prerelease \
  easy-wakeword.apk
```

### Option 3: Using Git Tag and GitHub Actions

If you have a workflow that builds on tags:

```bash
# Create and push the tag
git tag -a v1.0.0-poc -m "Proof of Concept release - Android 13/14"
git push origin v1.0.0-poc
```

## Verification

After creating the release, verify the download link works:
```
https://github.com/R00S/easy-android-ha-wakeword-app/releases/download/v1.0.0-poc/easy-wakeword.apk
```

This link is already referenced in the README.md file.
