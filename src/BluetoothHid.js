import { NativeModules, NativeEventEmitter, Platform } from 'react-native';

const { BluetoothHid } = NativeModules;

if (!BluetoothHid) {
  console.warn('BluetoothHid native module not found. Run on Android device.');
}

const emitter = BluetoothHid ? new NativeEventEmitter(BluetoothHid) : null;

export const BT_STATES = {
  DISCONNECTED: 0,
  CONNECTING: 1,
  CONNECTED: 2,
  DISCONNECTING: 3,
};

export default {
  initialize: () => BluetoothHid?.initialize(),
  isBluetoothEnabled: () => BluetoothHid?.isBluetoothEnabled(),
  startScan: () => BluetoothHid?.startScan(),
  stopScan: () => BluetoothHid?.stopScan(),
  getPairedDevices: () => BluetoothHid?.getPairedDevices(),
  connectDevice: (address) => BluetoothHid?.connectDevice(address),
  isConnected: () => BluetoothHid?.isConnected(),
  disconnectDevice: () => BluetoothHid?.disconnectDevice(),
  sendKey: (modifier, keyCode) => BluetoothHid?.sendKey(modifier, keyCode),
  // delayMs supports up to 5 decimals; randomness 0..1 adds human-like jitter.
  sendText: (text, delayMs, randomness = 0) =>
    BluetoothHid?.sendText(text, delayMs, randomness),
  stopAutoType: () => BluetoothHid?.stopAutoType(),

  addListener: (event, callback) => emitter?.addListener(event, callback),
};

// HID keycodes for special keys
export const KEY = {
  ENTER: 0x28,
  ESCAPE: 0x29,
  BACKSPACE: 0x2a,
  TAB: 0x2b,
  SPACE: 0x2c,
  CAPS_LOCK: 0x39,
  F1: 0x3a, F2: 0x3b, F3: 0x3c, F4: 0x3d,
  F5: 0x3e, F6: 0x3f, F7: 0x40, F8: 0x41,
  F9: 0x42, F10: 0x43, F11: 0x44, F12: 0x45,
  ARROW_RIGHT: 0x4f,
  ARROW_LEFT: 0x50,
  ARROW_DOWN: 0x51,
  ARROW_UP: 0x52,
  DELETE: 0x4c,
  HOME: 0x4a,
  END: 0x4d,
  PAGE_UP: 0x4b,
  PAGE_DOWN: 0x4e,
};

export const MOD = {
  NONE: 0x00,
  L_CTRL: 0x01,
  L_SHIFT: 0x02,
  L_ALT: 0x04,
  L_GUI: 0x08,
  R_CTRL: 0x10,
  R_SHIFT: 0x20,
  R_ALT: 0x40,
  R_GUI: 0x80,
};
