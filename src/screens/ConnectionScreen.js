import React, { useState, useEffect, useRef } from 'react';
import {
  View, Text, FlatList, TouchableOpacity,
  StyleSheet, ActivityIndicator, Alert,
  PermissionsAndroid, Platform,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import BluetoothHid, { BT_STATES } from '../BluetoothHid';

export default function ConnectionScreen({ navigation }) {
  const [ready, setReady] = useState(false);
  const [scanning, setScanning] = useState(false);
  const [devices, setDevices] = useState([]);
  const [pairedDevices, setPairedDevices] = useState([]);
  const [connectingAddress, setConnectingAddress] = useState(null);
  const listeners = useRef([]);

  useEffect(() => {
    setup();
    return () => listeners.current.forEach(l => l?.remove());
  }, []);

  async function setup() {
    await requestPermissions();
    await BluetoothHid.initialize();

    listeners.current.push(
      BluetoothHid.addListener('onHidReady', ({ ready }) => setReady(ready))
    );
    listeners.current.push(
      BluetoothHid.addListener('onDeviceFound', (device) => {
        setDevices(prev => {
          if (prev.find(d => d.address === device.address)) return prev;
          return [...prev, device];
        });
      })
    );
    listeners.current.push(
      BluetoothHid.addListener('onScanFinished', () => setScanning(false))
    );
    listeners.current.push(
      BluetoothHid.addListener('onConnectionStateChanged', ({ address, name, state }) => {
        setConnectingAddress(null);
        if (state === BT_STATES.CONNECTED) {
          navigation.navigate('Keyboard', { deviceName: name, deviceAddress: address });
        }
      })
    );

    loadPaired();
  }

  async function requestPermissions() {
    if (Platform.OS !== 'android') return;
    if (Platform.Version >= 31) {
      await PermissionsAndroid.requestMultiple([
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_SCAN,
        PermissionsAndroid.PERMISSIONS.BLUETOOTH_CONNECT,
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
      ]);
    } else {
      await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION
      );
    }
  }

  async function loadPaired() {
    const paired = await BluetoothHid.getPairedDevices();
    setPairedDevices(paired || []);
  }

  async function startScan() {
    setDevices([]);
    setScanning(true);
    await BluetoothHid.startScan();
  }

  async function connect(address, name) {
    setConnectingAddress(address);
    const ok = await BluetoothHid.connectDevice(address);
    if (!ok) {
      setConnectingAddress(null);
      Alert.alert('Connection Failed', `Could not connect to ${name}`);
    }
  }

  function DeviceItem({ item, isPaired }) {
    const isConnecting = connectingAddress === item.address;
    return (
      <TouchableOpacity
        style={styles.deviceItem}
        onPress={() => connect(item.address, item.name)}
        disabled={!!connectingAddress}
      >
        <View style={styles.deviceInfo}>
          <Text style={styles.deviceName}>{item.name}</Text>
          <Text style={styles.deviceAddress}>{item.address}</Text>
          {isPaired && <Text style={styles.pairedBadge}>Paired</Text>}
        </View>
        {isConnecting ? (
          <ActivityIndicator color="#6C63FF" />
        ) : (
          <Text style={styles.connectText}>Connect</Text>
        )}
      </TouchableOpacity>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>ConKey</Text>
        <Text style={styles.subtitle}>Bluetooth Keyboard</Text>
        <View style={[styles.statusDot, ready ? styles.dotReady : styles.dotNotReady]} />
      </View>

      <TouchableOpacity
        style={[styles.scanButton, scanning && styles.scanButtonActive]}
        onPress={scanning ? () => BluetoothHid.stopScan() : startScan}
      >
        {scanning
          ? <ActivityIndicator color="#fff" />
          : <Text style={styles.scanButtonText}>Scan for Devices</Text>
        }
      </TouchableOpacity>

      {pairedDevices.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Paired Devices</Text>
          <FlatList
            data={pairedDevices}
            keyExtractor={i => i.address}
            renderItem={({ item }) => <DeviceItem item={item} isPaired />}
          />
        </View>
      )}

      {devices.length > 0 && (
        <View style={styles.section}>
          <Text style={styles.sectionTitle}>Nearby Devices</Text>
          <FlatList
            data={devices}
            keyExtractor={i => i.address}
            renderItem={({ item }) => <DeviceItem item={item} isPaired={false} />}
          />
        </View>
      )}

      {!ready && (
        <View style={styles.notReadyBanner}>
          <Text style={styles.notReadyText}>Initializing Bluetooth HID…</Text>
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#121212' },
  header: { alignItems: 'center', paddingVertical: 24, paddingHorizontal: 16 },
  title: { fontSize: 32, fontWeight: '800', color: '#fff', letterSpacing: 2 },
  subtitle: { fontSize: 14, color: '#888', marginTop: 4 },
  statusDot: { width: 10, height: 10, borderRadius: 5, marginTop: 8 },
  dotReady: { backgroundColor: '#4CAF50' },
  dotNotReady: { backgroundColor: '#F44336' },
  scanButton: {
    margin: 16, padding: 16, borderRadius: 12,
    backgroundColor: '#6C63FF', alignItems: 'center',
  },
  scanButtonActive: { backgroundColor: '#4a43cc' },
  scanButtonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  section: { marginTop: 8, paddingHorizontal: 16 },
  sectionTitle: { color: '#888', fontSize: 12, fontWeight: '600', marginBottom: 8, textTransform: 'uppercase' },
  deviceItem: {
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
    backgroundColor: '#1e1e1e', padding: 14, borderRadius: 10, marginBottom: 8,
  },
  deviceInfo: { flex: 1 },
  deviceName: { color: '#fff', fontSize: 15, fontWeight: '600' },
  deviceAddress: { color: '#666', fontSize: 12, marginTop: 2 },
  pairedBadge: { color: '#6C63FF', fontSize: 11, marginTop: 4 },
  connectText: { color: '#6C63FF', fontWeight: '600' },
  notReadyBanner: {
    position: 'absolute', bottom: 0, left: 0, right: 0,
    backgroundColor: '#333', padding: 12, alignItems: 'center',
  },
  notReadyText: { color: '#aaa', fontSize: 13 },
});
