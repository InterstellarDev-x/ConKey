import React, { useState, useEffect, useRef } from 'react';
import {
  View, Text, TouchableOpacity, StyleSheet,
  Vibration, ScrollView, Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import BluetoothHid, { KEY, MOD, BT_STATES } from '../BluetoothHid';

const ROWS = [
  ['`', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '-', '=', 'DEL'],
  ['TAB', 'q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p', '[', ']', '\\'],
  ['CAPS', 'a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l', ';', '\'', 'ENTER'],
  ['SHIFT', 'z', 'x', 'c', 'v', 'b', 'n', 'm', ',', '.', '/', 'SHIFT'],
  ['CTRL', 'ALT', ' ', 'ALT', 'CTRL'],
];

const NAV_KEYS = ['←', '→', '↑', '↓', 'ESC', 'HOME', 'END', 'PgUp', 'PgDn', 'F1', 'F2', 'F3', 'F4', 'F5', 'F11', 'F12'];

const SPECIAL_KEY_MAP = {
  DEL: KEY.BACKSPACE,
  ENTER: KEY.ENTER,
  TAB: KEY.TAB,
  CAPS: KEY.CAPS_LOCK,
  ESC: KEY.ESCAPE,
  '←': KEY.ARROW_LEFT,
  '→': KEY.ARROW_RIGHT,
  '↑': KEY.ARROW_UP,
  '↓': KEY.ARROW_DOWN,
  HOME: KEY.HOME,
  END: KEY.END,
  PgUp: KEY.PAGE_UP,
  PgDn: KEY.PAGE_DOWN,
  F1: KEY.F1, F2: KEY.F2, F3: KEY.F3, F4: KEY.F4, F5: KEY.F5,
  F11: KEY.F11, F12: KEY.F12,
};

const MODIFIER_KEYS = new Set(['SHIFT', 'CTRL', 'ALT']);

// Map ASCII chars to HID keycodes
function charToHid(char) {
  const code = char.charCodeAt(0);
  if (code >= 97 && code <= 122) return { mod: MOD.NONE, key: code - 97 + 4 };
  if (code >= 65 && code <= 90) return { mod: MOD.L_SHIFT, key: code - 65 + 4 };
  if (code >= 49 && code <= 57) return { mod: MOD.NONE, key: code - 49 + 30 };
  if (char === '0') return { mod: MOD.NONE, key: 39 };
  if (char === ' ') return { mod: MOD.NONE, key: KEY.SPACE };
  if (char === '-') return { mod: MOD.NONE, key: 45 };
  if (char === '=') return { mod: MOD.NONE, key: 46 };
  if (char === '[') return { mod: MOD.NONE, key: 47 };
  if (char === ']') return { mod: MOD.NONE, key: 48 };
  if (char === '\\') return { mod: MOD.NONE, key: 49 };
  if (char === ';') return { mod: MOD.NONE, key: 51 };
  if (char === "'") return { mod: MOD.NONE, key: 52 };
  if (char === '`') return { mod: MOD.NONE, key: 53 };
  if (char === ',') return { mod: MOD.NONE, key: 54 };
  if (char === '.') return { mod: MOD.NONE, key: 55 };
  if (char === '/') return { mod: MOD.NONE, key: 56 };
  return null;
}

export default function KeyboardScreen({ route, navigation }) {
  const { deviceName, deviceAddress } = route.params || {};
  const [shiftActive, setShiftActive] = useState(false);
  const [ctrlActive, setCtrlActive] = useState(false);
  const [altActive, setAltActive] = useState(false);
  const [connected, setConnected] = useState(true);
  const listeners = useRef([]);

  useEffect(() => {
    listeners.current.push(
      BluetoothHid.addListener('onConnectionStateChanged', ({ address, state }) => {
        // Ignore disconnects for other devices — only react to our own.
        if (address && deviceAddress && address !== deviceAddress) return;
        if (state === BT_STATES.DISCONNECTED) {
          setConnected(false);
          Alert.alert('Disconnected', 'Bluetooth connection lost', [
            { text: 'OK', onPress: () => navigation.goBack() }
          ]);
        }
      })
    );
    return () => listeners.current.forEach(l => l?.remove());
  }, []);

  function buildModifier() {
    let mod = MOD.NONE;
    if (shiftActive) mod |= MOD.L_SHIFT;
    if (ctrlActive) mod |= MOD.L_CTRL;
    if (altActive) mod |= MOD.L_ALT;
    return mod;
  }

  function handleKey(label) {
    Vibration.vibrate(30);

    if (label === 'SHIFT') { setShiftActive(p => !p); return; }
    if (label === 'CTRL') { setCtrlActive(p => !p); return; }
    if (label === 'ALT') { setAltActive(p => !p); return; }

    const specialCode = SPECIAL_KEY_MAP[label];
    if (specialCode !== undefined) {
      BluetoothHid.sendKey(buildModifier(), specialCode);
    } else {
      const displayChar = shiftActive ? label.toUpperCase() : label;
      const hid = charToHid(displayChar);
      if (hid) {
        const mod = (shiftActive ? MOD.L_SHIFT : MOD.NONE) |
                    (ctrlActive ? MOD.L_CTRL : MOD.NONE) |
                    (altActive ? MOD.L_ALT : MOD.NONE);
        BluetoothHid.sendKey(mod, hid.key);
      }
    }

    // Auto-release sticky modifiers after use
    if (!MODIFIER_KEYS.has(label)) {
      setShiftActive(false);
      setCtrlActive(false);
      setAltActive(false);
    }
  }

  function keyWidth(label) {
    switch (label) {
      case 'DEL': case 'ENTER': case 'CAPS': return 2.2;
      case 'TAB': case '\\': return 1.8;
      case 'SHIFT': return 2.5;
      case 'CTRL': case 'ALT': return 1.5;
      case ' ': return 5;
      default: return 1;
    }
  }

  function isModActive(label) {
    if (label === 'SHIFT') return shiftActive;
    if (label === 'CTRL') return ctrlActive;
    if (label === 'ALT') return altActive;
    return false;
  }

  function renderKey(label, idx) {
    const flex = keyWidth(label);
    const modActive = isModActive(label);
    const isSpecial = MODIFIER_KEYS.has(label) || SPECIAL_KEY_MAP[label] !== undefined;
    return (
      <TouchableOpacity
        key={idx}
        style={[
          styles.key,
          { flex },
          isSpecial && styles.specialKey,
          modActive && styles.activeModKey,
          label === ' ' && styles.spaceKey,
        ]}
        onPress={() => handleKey(label)}
        activeOpacity={0.7}
      >
        <Text style={[styles.keyLabel, isSpecial && styles.specialLabel, modActive && styles.activeModLabel]}>
          {label === ' ' ? 'SPACE' : label}
        </Text>
      </TouchableOpacity>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Text style={styles.backText}>←</Text>
        </TouchableOpacity>
        <View style={styles.headerCenter}>
          <Text style={styles.deviceName} numberOfLines={1}>{deviceName}</Text>
          <View style={[styles.connDot, connected ? styles.dotGreen : styles.dotRed]} />
        </View>
        <TouchableOpacity
          style={styles.autoTypeBtn}
          onPress={() => navigation.navigate('AutoType', { deviceName, deviceAddress })}
        >
          <Text style={styles.autoTypeBtnText}>Auto-type</Text>
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.keyboard}>
        {ROWS.map((row, ri) => (
          <View key={ri} style={styles.row}>
            {row.map((key, ki) => renderKey(key, ki))}
          </View>
        ))}
        <View style={styles.row}>
          {NAV_KEYS.map((key, ki) => renderKey(key, ki))}
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212' },
  header: {
    flexDirection: 'row', alignItems: 'center',
    paddingHorizontal: 12, paddingVertical: 10,
    borderBottomWidth: 1, borderBottomColor: '#222',
  },
  backBtn: { padding: 4 },
  backText: { color: '#6C63FF', fontSize: 22 },
  headerCenter: { flex: 1, flexDirection: 'row', alignItems: 'center', marginHorizontal: 12 },
  deviceName: { color: '#fff', fontSize: 15, fontWeight: '600', flex: 1 },
  connDot: { width: 8, height: 8, borderRadius: 4, marginLeft: 6 },
  dotGreen: { backgroundColor: '#4CAF50' },
  dotRed: { backgroundColor: '#F44336' },
  autoTypeBtn: {
    backgroundColor: '#6C63FF', paddingHorizontal: 12,
    paddingVertical: 6, borderRadius: 8,
  },
  autoTypeBtnText: { color: '#fff', fontWeight: '600', fontSize: 13 },
  keyboard: { padding: 8 },
  row: { flexDirection: 'row', marginBottom: 6, justifyContent: 'center' },
  key: {
    backgroundColor: '#2a2a2a', borderRadius: 8,
    paddingVertical: 12, marginHorizontal: 2,
    alignItems: 'center', justifyContent: 'center',
    minWidth: 28,
  },
  specialKey: { backgroundColor: '#333' },
  activeModKey: { backgroundColor: '#6C63FF' },
  spaceKey: { backgroundColor: '#2a2a2a' },
  keyLabel: { color: '#e0e0e0', fontSize: 13, fontWeight: '500' },
  specialLabel: { color: '#aaa', fontSize: 11 },
  activeModLabel: { color: '#fff' },
});
