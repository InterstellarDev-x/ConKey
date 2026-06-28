# Architecture

ConKey is a thin React Native UI on top of a custom Kotlin native module that
does the actual Bluetooth HID work.

```
┌─────────────────────────────────────────────┐
│  React Native (JS)                            │
│                                               │
│  App.js  ──  native-stack navigator           │
│    ├── ConnectionScreen   scan / pair / connect│
│    ├── KeyboardScreen      QWERTY + modifiers   │
│    └── AutoTypeScreen      paste + auto-type     │
│                                               │
│  src/BluetoothHid.js   JS bridge + keycodes    │
└───────────────┬───────────────────────────────┘
                │  NativeModules + NativeEventEmitter
┌───────────────┴───────────────────────────────┐
│  Native (Kotlin, Android)                      │
│                                               │
│  BluetoothHidModule.kt   the real work          │
│  BluetoothHidPackage.kt  registers the module   │
│  MainApplication.kt      adds the package       │
└───────────────┬───────────────────────────────┘
                │  android.bluetooth.BluetoothHidDevice (API 28+)
        ┌───────┴────────┐
        │  Host device   │  laptop / TV / tablet
        └────────────────┘
```

## JavaScript layer

| File | Responsibility |
|---|---|
| [App.js](../App.js) | Sets up the navigation container and the three-screen stack. Dark theme, no headers. |
| [src/BluetoothHid.js](../src/BluetoothHid.js) | The single bridge to the native module. Exposes promise-based methods (`initialize`, `startScan`, `connectDevice`, `sendKey`, `sendText`, …), the `addListener` event helper, and the `KEY` / `MOD` / `BT_STATES` constant maps. |
| [src/screens/ConnectionScreen.js](../src/screens/ConnectionScreen.js) | Requests permissions, initializes the module, lists paired + discovered devices, and drives connection. Navigates to Keyboard on `STATE_CONNECTED`. |
| [src/screens/KeyboardScreen.js](../src/screens/KeyboardScreen.js) | Renders the QWERTY grid, tracks sticky modifiers (Shift/Ctrl/Alt), maps each key to an HID keycode, and fires haptics. |
| [src/screens/AutoTypeScreen.js](../src/screens/AutoTypeScreen.js) | Text box + speed/randomness controls. Calls `sendText` and shows live progress. |

### Events (native → JS)

The native module emits these via `RCTDeviceEventEmitter`; screens subscribe
through `BluetoothHid.addListener`:

| Event | Payload | Meaning |
|---|---|---|
| `onHidReady` | `{ ready }` | HID profile proxy connected/disconnected. |
| `onHidAppStatus` | `{ registered, device }` | HID app registration changed. |
| `onConnectionStateChanged` | `{ address, name, state }` | Host connect/disconnect (`state` per `BT_STATES`). |
| `onDeviceFound` | `{ address, name }` | A device was discovered during a scan. |
| `onScanFinished` | `{}` | Discovery ended. |
| `onAutoTypeProgress` | `{ sent, total }` | Live auto-type progress. |

## Native layer

| File | Responsibility |
|---|---|
| [BluetoothHidModule.kt](../android/app/src/main/java/com/conkey/BluetoothHidModule.kt) | All Bluetooth HID logic — see [bluetooth-hid.md](bluetooth-hid.md). |
| [BluetoothHidPackage.kt](../android/app/src/main/java/com/conkey/BluetoothHidPackage.kt) | A `ReactPackage` that exposes the module to React Native. |
| [MainApplication.kt](../android/app/src/main/java/com/conkey/MainApplication.kt) | Adds `BluetoothHidPackage()` to the package list. |

## Threading model

- **JS bridge calls** return immediately with a Promise.
- **Single keystrokes** (`sendKey`) run on a single-thread `Executor` so the
  12 ms key-hold sleep never blocks the JS thread.
- **Auto-type** (`sendText`) runs on its own interruptible `Thread`, so the user
  can stop mid-stream; calling it again interrupts the previous run first.
- **HID callbacks** are delivered on the same `Executor`.

## Key design decisions

- **Android only, for now.** iOS can't expose a custom Bluetooth HID keyboard
  from a normal app — see [bluetooth-hid.md](bluetooth-hid.md#ios).
- **Bare workflow, not managed.** `BluetoothHidDevice` is not in any Expo SDK or
  community library, so a custom native module is unavoidable.
- **No backend.** Fully local: pair, type, done. No accounts, no sync.
