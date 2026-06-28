# Bluetooth HID — how it works & the gotchas

ConKey makes the phone act as a Bluetooth **HID** (Human Interface Device) —
specifically a keyboard. The host (laptop/TV/tablet) sees a standard keyboard
and needs no special software.

This is all done through Android's [`BluetoothHidDevice`](https://developer.android.com/reference/android/bluetooth/BluetoothHidDevice)
API, available since **Android 9 (API 28)**. It is a public API — no root, no
system signature, no `BLUETOOTH_PRIVILEGED` required.

## Compatibility — which hosts work

Because ConKey advertises the **standard HID keyboard** profile, it works with
essentially any device that accepts a Bluetooth keyboard:

| Host | Works? | Notes |
|---|---|---|
| Windows | ✅ | Pair from Settings → Bluetooth. |
| **macOS** | ✅ | Pair from System Settings → Bluetooth; appears as "ConKey". |
| Linux | ✅ | Standard HID, no driver needed. |
| Android (as host) | ✅ | One Android phone typing into another. |
| Smart TVs | ✅ (most) | Some OEMs restrict HID hosts. |
| iPhone/iPad (as host) | ✅ | iOS happily *accepts* external keyboards. |

> The phone running ConKey must be Android 9+. The host can be anything that
> takes a Bluetooth keyboard — including macOS.

## The setup flow (native)

1. **Get the profile proxy** — `BluetoothAdapter.getProfileProxy(..., HID_DEVICE)`.
2. **Register the HID app** — once the proxy connects, call `registerApp()` with:
   - an SDP record (name "ConKey", `SUBCLASS1_KEYBOARD`),
   - the **HID report descriptor** — the byte array that declares "I am a
     standard 8-byte boot keyboard" (modifier byte + reserved + 6 keycodes).
3. **Bond + connect** — pair with the host (`createBond()`), then `connect()`.
4. **Send keystrokes** — `sendReport()` with the 8-byte report.

## The 8-byte keyboard report

```
byte 0  modifier bitmask (Ctrl/Shift/Alt/GUI, left & right)
byte 1  reserved (always 0)
byte 2  keycode 1
byte 3  keycode 2
...      (up to 6 simultaneous keys)
byte 7  keycode 6
```

A keystroke is **press → hold → release**:
- press report: modifier + keycode,
- release report: all zeros.

## Gotchas we hit (and fixed)

These are the non-obvious failures that cost the most time:

### 1. You must bond before connecting
`connect()` on an **unbonded** device fails silently. ConKey calls
`createBond()` first and waits for `ACTION_BOND_STATE_CHANGED == BONDED` before
connecting. See `connectDevice()` in
[BluetoothHidModule.kt](../android/app/src/main/java/com/conkey/BluetoothHidModule.kt).

### 2. Registration is asynchronous
`registerApp()` doesn't finish synchronously — `onAppStatusChanged(registered=true)`
fires later. Calling `connect()` before that is a no-op. ConKey stashes the
target in `pendingConnect` and connects once registration completes.

### 3. Discovery starves connection
An active Bluetooth scan steals the radio. ConKey calls `cancelDiscovery()`
before any connection attempt.

### 4. Too-fast release = double characters
If the release report follows the press with no gap, the host can miss the
press *or* trigger key auto-repeat — so one tap types two or more characters.
ConKey holds each key ~12 ms and **always** sends the release in a `finally`
block, so a key can never stick down. This was the "one bracket types twice" bug.

### 5. Sub-millisecond timing needs nanos
`Thread.sleep(long)` can't do fractional milliseconds. For the 5-decimal delay
precision, ConKey uses `Thread.sleep(millis, nanos)` (see `sleepPrecise()`).

### 6. Flooding `sendReport()` drops the connection
This caused the "auto-disconnects while/after typing" bug. `sendReport()`
returns `false` when the radio's L2CAP buffer is full. If you ignore that and
keep pushing reports, the buffer overflows and the **stack drops the whole
connection** to recover. Three mitigations, all in
[BluetoothHidModule.kt](../android/app/src/main/java/com/conkey/BluetoothHidModule.kt):
- `sendReportFlowControlled()` checks the return value and backs off + retries
  when the buffer is full, instead of blindly flooding.
- A **5 ms floor** between characters in the auto-type loop, so a tiny/zero
  delay can't saturate the radio.
- **Best-effort QoS** registered in `registerApp()` so the stack tolerates
  short bursts.
Also: the auto-type loop now checks the live connection each iteration and stops
if the host disconnected, rather than firing reports at a dead link.

### 7. Stale `connectedDevice` cache → false "no device connected"
Symptom: typing stops working mid-session with "no device connected", but the
header still shows the device name, and reopening the app fixes it. Cause: a
transient `STATE_DISCONNECTED` nulled the cached `connectedDevice`, but the host
stayed connected (or silently reconnected) at the HID level without a fresh
`STATE_CONNECTED` callback — so the cache was wrongly `null` while the link was
alive. Fix: `resolveConnectedDevice()` falls back to the authoritative
`hidDevice.getConnectedDevices()` and re-caches it, and `sendKey`/`sendText` use
it instead of the raw cache. There's also an `isConnected()` method for the UI
to re-verify the live state.

## Permissions

Declared in [AndroidManifest.xml](../android/app/src/main/AndroidManifest.xml):

```xml
<!-- Android 11 and lower -->
<uses-permission android:name="android.permission.BLUETOOTH" android:maxSdkVersion="30"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" android:maxSdkVersion="30"/>
<!-- Android 12+ -->
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT"/>
<uses-permission android:name="android.permission.BLUETOOTH_SCAN"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
```

On Android 12+ (`BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN`) these are **runtime**
permissions — `ConnectionScreen` requests them at startup.

## iOS

iOS is **not supported** as the device running ConKey. Apple does not let a
normal app expose a custom Bluetooth HID peripheral (it requires the MFi
hardware program). An iPhone can *receive* keystrokes from ConKey (it's a fine
host), but it can't *be* ConKey. This is a platform limitation, not a code one.
