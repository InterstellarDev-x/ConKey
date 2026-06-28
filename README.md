# ConKey

Turn your Android phone into a Bluetooth keyboard for any device.

ConKey makes your phone act as a standard Bluetooth **HID keyboard**, so any
laptop, PC, smart TV, or tablet sees it as a normal keyboard — no drivers, no
app needed on the other device. Built for people who don't have a physical
keyboard to connect to a laptop or other device.

## Features

- **Full QWERTY keyboard** — letters, numbers, symbols, plus modifiers
  (Ctrl, Alt, Shift), arrow keys, and function keys.
- **Auto-type** — paste a block of text and ConKey types it out
  keystroke-by-keystroke. Useful for passwords, code snippets, and long text.
  - **Configurable delay** between characters, down to 5-decimal millisecond
    precision (e.g. `49.37512 ms`).
  - **Random speed** — optional human-like jitter so typing isn't robotically even.
- **Remembers paired devices** and auto-reconnects.
- **Haptic feedback** and visual key highlights.
- **Dark, minimal UI.**

## Requirements

- **Android 9 (API 28) or newer** — required for the `BluetoothHidDevice` API.
- A target device that accepts Bluetooth keyboards (virtually all do).

## Quick start

This is an **Expo bare-workflow** app with a custom native Android module, so it
must be built — it can't run in Expo Go.

The easiest path (no Android Studio or JDK needed) is **EAS Build**, which
compiles the APK in the cloud:

```bash
npm install -g eas-cli
eas login
cd ConKey
eas build --platform android --profile preview
```

EAS returns a download link for the APK. Open it on your Android phone to install.

For local builds and the full toolchain, see [docs/build-and-deploy.md](docs/build-and-deploy.md).

## How to use

1. Open ConKey and grant the Bluetooth + Location permissions.
2. On the **Connection** screen, tap **Scan** (or pick a paired device) and tap
   a device to connect. Accept the pairing prompt on both devices the first time.
3. Once connected you land on the **Keyboard** screen — start typing.
4. Tap **Auto-type** to paste text, set the speed/randomness, and send it.

## Documentation

- [docs/architecture.md](docs/architecture.md) — how the app is structured (JS + native).
- [docs/bluetooth-hid.md](docs/bluetooth-hid.md) — how Bluetooth HID works here, and the gotchas.
- [docs/build-and-deploy.md](docs/build-and-deploy.md) — building the APK, cloud and local.

## Tech stack

| Layer | Choice |
|---|---|
| Framework | React Native (Expo bare workflow) |
| Navigation | `@react-navigation/native-stack` |
| Native module | Kotlin, wrapping Android `BluetoothHidDevice` (API 28+) |
| Build | EAS Build (cloud) or Gradle (local) |
| Distribution | APK sideload (MVP) |

## Roadmap

- iOS support (requires a different approach — see bluetooth-hid.md).
- Media keys and a numeric keypad.
- Light theme.
- LLM-assisted features (smart autocomplete / text rewrite).
- Play Store release.
