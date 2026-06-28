import React, { useState, useEffect, useRef } from 'react';
import {
  View, Text, TextInput, TouchableOpacity,
  StyleSheet, ScrollView, Alert,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import BluetoothHid from '../BluetoothHid';

const DELAY_PRESETS = [
  { label: 'Fast', value: 20 },
  { label: 'Normal', value: 50 },
  { label: 'Slow', value: 150 },
  { label: 'Very Slow', value: 300 },
];

const RANDOM_PRESETS = [
  { label: 'Off', value: 0 },
  { label: 'Light', value: 0.25 },
  { label: 'Human', value: 0.5 },
  { label: 'Wild', value: 0.9 },
];

export default function AutoTypeScreen({ route, navigation }) {
  const { deviceName } = route.params || {};
  const [text, setText] = useState('');
  const [delayMs, setDelayMs] = useState(50);
  // Separate raw text so users can type partial values like "49." without snapping.
  const [delayText, setDelayText] = useState('50');
  const [randomness, setRandomness] = useState(0.5);
  const [typing, setTyping] = useState(false);
  const [progress, setProgress] = useState('');
  const progressListener = useRef(null);

  function onDelayTextChange(raw) {
    // Keep only digits and a single decimal point, max 5 decimal places.
    let cleaned = raw.replace(/[^0-9.]/g, '');
    const firstDot = cleaned.indexOf('.');
    if (firstDot !== -1) {
      const intPart = cleaned.slice(0, firstDot);
      let decPart = cleaned.slice(firstDot + 1).replace(/\./g, '').slice(0, 5);
      cleaned = `${intPart}.${decPart}`;
    }
    setDelayText(cleaned);
    const parsed = parseFloat(cleaned);
    if (!isNaN(parsed)) setDelayMs(parsed);
  }

  // Keep the custom-delay text in sync when a preset button is tapped.
  function selectDelayPreset(value) {
    setDelayMs(value);
    setDelayText(String(value));
  }

  useEffect(() => {
    progressListener.current = BluetoothHid.addListener(
      'onAutoTypeProgress',
      ({ sent, total }) => {
        if (sent < total) setProgress(`Typing… ${sent}/${total}`);
      }
    );
    return () => progressListener.current?.remove();
  }, []);

  async function startAutoType() {
    if (!text.trim()) {
      Alert.alert('No text', 'Paste or type the text you want to send.');
      return;
    }
    setTyping(true);
    setProgress(`Sending ${text.length} characters…`);
    try {
      const ok = await BluetoothHid.sendText(text, delayMs, randomness);
      setProgress(ok ? 'Done!' : 'Stopped.');
    } catch (e) {
      setProgress('Error: ' + e.message);
    } finally {
      setTyping(false);
    }
  }

  async function stopAutoType() {
    await BluetoothHid.stopAutoType();
    setTyping(false);
    setProgress('Stopped.');
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Text style={styles.backText}>←</Text>
        </TouchableOpacity>
        <Text style={styles.title}>Auto-type</Text>
        <Text style={styles.deviceName} numberOfLines={1}>{deviceName}</Text>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.label}>Text to type</Text>
        <TextInput
          style={styles.textInput}
          value={text}
          onChangeText={setText}
          multiline
          placeholder="Paste your text here…"
          placeholderTextColor="#555"
          editable={!typing}
          textAlignVertical="top"
        />

        <Text style={styles.label}>Typing speed</Text>
        <View style={styles.presets}>
          {DELAY_PRESETS.map(preset => (
            <TouchableOpacity
              key={preset.value}
              style={[styles.preset, delayMs === preset.value && styles.presetActive]}
              onPress={() => selectDelayPreset(preset.value)}
            >
              <Text style={[styles.presetLabel, delayMs === preset.value && styles.presetLabelActive]}>
                {preset.label}
              </Text>
              <Text style={[styles.presetSub, delayMs === preset.value && styles.presetLabelActive]}>
                {preset.value}ms
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        <View style={styles.customDelayRow}>
          <Text style={styles.customDelayLabel}>Custom delay (ms):</Text>
          <TextInput
            style={styles.customDelayInput}
            value={delayText}
            onChangeText={onDelayTextChange}
            keyboardType="decimal-pad"
            editable={!typing}
            placeholder="50.00000"
            placeholderTextColor="#555"
          />
        </View>
        <Text style={styles.hint}>Up to 5 decimals (e.g. 49.37512 ms)</Text>

        <Text style={styles.label}>Randomness</Text>
        <View style={styles.presets}>
          {RANDOM_PRESETS.map(preset => (
            <TouchableOpacity
              key={preset.value}
              style={[styles.preset, randomness === preset.value && styles.presetActive]}
              onPress={() => setRandomness(preset.value)}
            >
              <Text style={[styles.presetLabel, randomness === preset.value && styles.presetLabelActive]}>
                {preset.label}
              </Text>
              <Text style={[styles.presetSub, randomness === preset.value && styles.presetLabelActive]}>
                ±{Math.round(preset.value * 100)}%
              </Text>
            </TouchableOpacity>
          ))}
        </View>

        {progress ? (
          <View style={styles.progressBanner}>
            <Text style={styles.progressText}>{progress}</Text>
          </View>
        ) : null}

        <View style={styles.charCount}>
          <Text style={styles.charCountText}>{text.length} characters</Text>
        </View>
      </ScrollView>

      <View style={styles.footer}>
        {typing ? (
          <TouchableOpacity style={[styles.actionBtn, styles.stopBtn]} onPress={stopAutoType}>
            <Text style={styles.actionBtnText}>Stop</Text>
          </TouchableOpacity>
        ) : (
          <TouchableOpacity
            style={[styles.actionBtn, !text.trim() && styles.actionBtnDisabled]}
            onPress={startAutoType}
            disabled={!text.trim()}
          >
            <Text style={styles.actionBtnText}>Send Text</Text>
          </TouchableOpacity>
        )}
      </View>
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
  title: { color: '#fff', fontSize: 17, fontWeight: '700', marginHorizontal: 10 },
  deviceName: { flex: 1, color: '#888', fontSize: 13, textAlign: 'right' },
  content: { padding: 16 },
  label: { color: '#888', fontSize: 12, fontWeight: '600', textTransform: 'uppercase', marginBottom: 8, marginTop: 16 },
  textInput: {
    backgroundColor: '#1e1e1e', color: '#fff', borderRadius: 10,
    padding: 14, fontSize: 15, minHeight: 150,
    borderWidth: 1, borderColor: '#333',
  },
  presets: { flexDirection: 'row', gap: 8 },
  preset: {
    flex: 1, backgroundColor: '#1e1e1e', borderRadius: 10,
    padding: 12, alignItems: 'center', borderWidth: 1, borderColor: '#333',
  },
  presetActive: { borderColor: '#6C63FF', backgroundColor: '#1a183a' },
  presetLabel: { color: '#aaa', fontWeight: '600', fontSize: 13 },
  presetLabelActive: { color: '#6C63FF' },
  presetSub: { color: '#555', fontSize: 11, marginTop: 2 },
  customDelayRow: {
    flexDirection: 'row', alignItems: 'center',
    marginTop: 12, backgroundColor: '#1e1e1e',
    borderRadius: 10, padding: 12,
  },
  customDelayLabel: { color: '#aaa', flex: 1 },
  hint: { color: '#555', fontSize: 11, marginTop: 6 },
  customDelayInput: {
    color: '#fff', borderBottomWidth: 1, borderBottomColor: '#6C63FF',
    textAlign: 'center', width: 110, fontSize: 16,
  },
  progressBanner: {
    marginTop: 16, backgroundColor: '#1e3a1e', borderRadius: 8, padding: 12,
  },
  progressText: { color: '#4CAF50', fontWeight: '600' },
  charCount: { marginTop: 8, alignItems: 'flex-end' },
  charCountText: { color: '#555', fontSize: 12 },
  footer: { padding: 16 },
  actionBtn: {
    backgroundColor: '#6C63FF', padding: 16,
    borderRadius: 12, alignItems: 'center',
  },
  stopBtn: { backgroundColor: '#c0392b' },
  actionBtnDisabled: { backgroundColor: '#333' },
  actionBtnText: { color: '#fff', fontSize: 16, fontWeight: '700' },
});
