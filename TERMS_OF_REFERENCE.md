# Terms of Reference (ToR)
## Project
Android helper app for always-on wake word to trigger Home Assistant Assist, using Automate + Hotword Plugin.

## Goal
Deliver a minimal-tap setup on old Android phones/tablets: install Hotword Plugin and Automate, import a bundled Automate flow, load a default wake-word model (“Hey Mycroft”), and ensure the wake word opens Home Assistant Assist (or the HA app) with no typing required.

## Scope
- Build a small Kotlin Android helper app (single-activity wizard).
- Ship one Automate `.flo` (always-on only; no screen/headset/charging gating).
- Bundle a permissively licensed wake-word model (“Hey Mycroft”, <8 MB) and import it into Hotword Plugin via its public import/share intent.
- Keep everything on-device; no user typing required.

## Wizard Steps (UX)
1) Install Hotword Plugin (Play Store deep link), then Continue.
2) Install Automate (Play Store deep link), then Continue.
3) Import bundled `.flo` into Automate via its import intent (one tap).
4) Enable the flow (open Automate with the flow preselected; user toggles on).
5) Load the wake-word model into Hotword Plugin via share/import (one tap).

## Automate Flow Behavior
- Trigger: Hotword Plugin broadcast/intent on wake-word detection.
- Action: Launch HA Assist via intent/deeplink if available; fallback to open HA app Assist screen or main activity.
- Optional webhook/audio block included but disabled by default.
- Foreground/persistent notification to stay alive; no gating (no screen/headset/charging checks).

## Permissions and Reliability
- Mic permission is requested by Hotword Plugin.
- Helper app requests POST_NOTIFICATIONS (Android 13+) and opens battery-optimization exemption settings.
- Optional Quick Settings tile to toggle the Automate flow via intent.

## Deliverables
- Android helper app source + build instructions.
- Bundled Automate `.flo` file and import wiring.
- Bundled wake-word model and import/share flow.
- README with tap-by-tap sequence and HA intent/deeplink constants.

## Acceptance Criteria
- On a clean device, with no typing: install helper → follow prompts → end with always-listening wake word that opens HA Assist (or HA app) on detection.
