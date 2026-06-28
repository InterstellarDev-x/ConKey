package com.conkey

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import java.util.concurrent.Executors

class BluetoothHidModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    companion object {
        const val NAME = "BluetoothHid"

        // HID keyboard report descriptor (boot-compatible keyboard)
        val KEYBOARD_DESCRIPTOR = byteArrayOf(
            0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
            0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
            0xa1.toByte(), 0x01.toByte(), // Collection (Application)
            0x05.toByte(), 0x07.toByte(), // Usage Page (Keyboard)
            0x19.toByte(), 0xe0.toByte(), // Usage Minimum (Keyboard LeftControl)
            0x29.toByte(), 0xe7.toByte(), // Usage Maximum (Keyboard Right GUI)
            0x15.toByte(), 0x00.toByte(), // Logical Minimum (0)
            0x25.toByte(), 0x01.toByte(), // Logical Maximum (1)
            0x75.toByte(), 0x01.toByte(), // Report Size (1)
            0x95.toByte(), 0x08.toByte(), // Report Count (8)
            0x81.toByte(), 0x02.toByte(), // Input (Data, Variable, Absolute)
            0x95.toByte(), 0x01.toByte(), // Report Count (1)
            0x75.toByte(), 0x08.toByte(), // Report Size (8)
            0x81.toByte(), 0x01.toByte(), // Input (Constant) - reserved byte
            0x95.toByte(), 0x06.toByte(), // Report Count (6)
            0x75.toByte(), 0x08.toByte(), // Report Size (8)
            0x15.toByte(), 0x00.toByte(), // Logical Minimum (0)
            0x25.toByte(), 0x65.toByte(), // Logical Maximum (101)
            0x05.toByte(), 0x07.toByte(), // Usage Page (Keyboard)
            0x19.toByte(), 0x00.toByte(), // Usage Minimum (0)
            0x29.toByte(), 0x65.toByte(), // Usage Maximum (101)
            0x81.toByte(), 0x00.toByte(), // Input (Data, Array, Absolute)
            0xc0.toByte()                 // End Collection
        )

        // USB HID keycodes for printable ASCII characters (offset from space 0x20)
        val ASCII_TO_KEYCODE = mapOf(
            ' ' to Pair(0x00.toByte(), 0x2c.toByte()),
            '!' to Pair(0x02.toByte(), 0x1e.toByte()),
            '"' to Pair(0x02.toByte(), 0x34.toByte()),
            '#' to Pair(0x02.toByte(), 0x20.toByte()),
            '$' to Pair(0x02.toByte(), 0x21.toByte()),
            '%' to Pair(0x02.toByte(), 0x22.toByte()),
            '&' to Pair(0x02.toByte(), 0x24.toByte()),
            '\'' to Pair(0x00.toByte(), 0x34.toByte()),
            '(' to Pair(0x02.toByte(), 0x26.toByte()),
            ')' to Pair(0x02.toByte(), 0x27.toByte()),
            '*' to Pair(0x02.toByte(), 0x25.toByte()),
            '+' to Pair(0x02.toByte(), 0x2e.toByte()),
            ',' to Pair(0x00.toByte(), 0x36.toByte()),
            '-' to Pair(0x00.toByte(), 0x2d.toByte()),
            '.' to Pair(0x00.toByte(), 0x37.toByte()),
            '/' to Pair(0x00.toByte(), 0x38.toByte()),
            '0' to Pair(0x00.toByte(), 0x27.toByte()),
            '1' to Pair(0x00.toByte(), 0x1e.toByte()),
            '2' to Pair(0x00.toByte(), 0x1f.toByte()),
            '3' to Pair(0x00.toByte(), 0x20.toByte()),
            '4' to Pair(0x00.toByte(), 0x21.toByte()),
            '5' to Pair(0x00.toByte(), 0x22.toByte()),
            '6' to Pair(0x00.toByte(), 0x23.toByte()),
            '7' to Pair(0x00.toByte(), 0x24.toByte()),
            '8' to Pair(0x00.toByte(), 0x25.toByte()),
            '9' to Pair(0x00.toByte(), 0x26.toByte()),
            ':' to Pair(0x02.toByte(), 0x33.toByte()),
            ';' to Pair(0x00.toByte(), 0x33.toByte()),
            '<' to Pair(0x02.toByte(), 0x36.toByte()),
            '=' to Pair(0x00.toByte(), 0x2e.toByte()),
            '>' to Pair(0x02.toByte(), 0x37.toByte()),
            '?' to Pair(0x02.toByte(), 0x38.toByte()),
            '@' to Pair(0x02.toByte(), 0x1f.toByte()),
            'A' to Pair(0x02.toByte(), 0x04.toByte()),
            'B' to Pair(0x02.toByte(), 0x05.toByte()),
            'C' to Pair(0x02.toByte(), 0x06.toByte()),
            'D' to Pair(0x02.toByte(), 0x07.toByte()),
            'E' to Pair(0x02.toByte(), 0x08.toByte()),
            'F' to Pair(0x02.toByte(), 0x09.toByte()),
            'G' to Pair(0x02.toByte(), 0x0a.toByte()),
            'H' to Pair(0x02.toByte(), 0x0b.toByte()),
            'I' to Pair(0x02.toByte(), 0x0c.toByte()),
            'J' to Pair(0x02.toByte(), 0x0d.toByte()),
            'K' to Pair(0x02.toByte(), 0x0e.toByte()),
            'L' to Pair(0x02.toByte(), 0x0f.toByte()),
            'M' to Pair(0x02.toByte(), 0x10.toByte()),
            'N' to Pair(0x02.toByte(), 0x11.toByte()),
            'O' to Pair(0x02.toByte(), 0x12.toByte()),
            'P' to Pair(0x02.toByte(), 0x13.toByte()),
            'Q' to Pair(0x02.toByte(), 0x14.toByte()),
            'R' to Pair(0x02.toByte(), 0x15.toByte()),
            'S' to Pair(0x02.toByte(), 0x16.toByte()),
            'T' to Pair(0x02.toByte(), 0x17.toByte()),
            'U' to Pair(0x02.toByte(), 0x18.toByte()),
            'V' to Pair(0x02.toByte(), 0x19.toByte()),
            'W' to Pair(0x02.toByte(), 0x1a.toByte()),
            'X' to Pair(0x02.toByte(), 0x1b.toByte()),
            'Y' to Pair(0x02.toByte(), 0x1c.toByte()),
            'Z' to Pair(0x02.toByte(), 0x1d.toByte()),
            '[' to Pair(0x00.toByte(), 0x2f.toByte()),
            '\\' to Pair(0x00.toByte(), 0x31.toByte()),
            ']' to Pair(0x00.toByte(), 0x30.toByte()),
            '^' to Pair(0x02.toByte(), 0x23.toByte()),
            '_' to Pair(0x02.toByte(), 0x2d.toByte()),
            '`' to Pair(0x00.toByte(), 0x35.toByte()),
            'a' to Pair(0x00.toByte(), 0x04.toByte()),
            'b' to Pair(0x00.toByte(), 0x05.toByte()),
            'c' to Pair(0x00.toByte(), 0x06.toByte()),
            'd' to Pair(0x00.toByte(), 0x07.toByte()),
            'e' to Pair(0x00.toByte(), 0x08.toByte()),
            'f' to Pair(0x00.toByte(), 0x09.toByte()),
            'g' to Pair(0x00.toByte(), 0x0a.toByte()),
            'h' to Pair(0x00.toByte(), 0x0b.toByte()),
            'i' to Pair(0x00.toByte(), 0x0c.toByte()),
            'j' to Pair(0x00.toByte(), 0x0d.toByte()),
            'k' to Pair(0x00.toByte(), 0x0e.toByte()),
            'l' to Pair(0x00.toByte(), 0x0f.toByte()),
            'm' to Pair(0x00.toByte(), 0x10.toByte()),
            'n' to Pair(0x00.toByte(), 0x11.toByte()),
            'o' to Pair(0x00.toByte(), 0x12.toByte()),
            'p' to Pair(0x00.toByte(), 0x13.toByte()),
            'q' to Pair(0x00.toByte(), 0x14.toByte()),
            'r' to Pair(0x00.toByte(), 0x15.toByte()),
            's' to Pair(0x00.toByte(), 0x16.toByte()),
            't' to Pair(0x00.toByte(), 0x17.toByte()),
            'u' to Pair(0x00.toByte(), 0x18.toByte()),
            'v' to Pair(0x00.toByte(), 0x19.toByte()),
            'w' to Pair(0x00.toByte(), 0x1a.toByte()),
            'x' to Pair(0x00.toByte(), 0x1b.toByte()),
            'y' to Pair(0x00.toByte(), 0x1c.toByte()),
            'z' to Pair(0x00.toByte(), 0x1d.toByte()),
            '{' to Pair(0x02.toByte(), 0x2f.toByte()),
            '|' to Pair(0x02.toByte(), 0x31.toByte()),
            '}' to Pair(0x02.toByte(), 0x30.toByte()),
            '~' to Pair(0x02.toByte(), 0x35.toByte()),
            '\n' to Pair(0x00.toByte(), 0x28.toByte()),
            '\t' to Pair(0x00.toByte(), 0x2b.toByte())
        )
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var hidDevice: BluetoothHidDevice? = null
    private var connectedDevice: BluetoothDevice? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var autoTypeThread: Thread? = null

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            sendEvent("onHidAppStatus", Arguments.createMap().apply {
                putBoolean("registered", registered)
                putString("device", pluggedDevice?.address)
            })
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                if (connectedDevice?.address == device.address) connectedDevice = null
            }
            sendEvent("onConnectionStateChanged", Arguments.createMap().apply {
                putString("address", device.address)
                putString("name", device.name ?: "Unknown")
                putInt("state", state)
            })
        }

        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            hidDevice?.replyReport(device, type, id, ByteArray(8))
        }

        override fun onSetReport(device: BluetoothDevice, type: Byte, id: Byte, data: ByteArray) {
            hidDevice?.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS)
        }
    }

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = proxy as BluetoothHidDevice
                val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                    "ConKey",
                    "Bluetooth Keyboard",
                    "ConKey App",
                    BluetoothHidDevice.SUBCLASS1_KEYBOARD,
                    KEYBOARD_DESCRIPTOR
                )
                hidDevice?.registerApp(sdpSettings, null, null, executor, hidCallback)
                sendEvent("onHidReady", Arguments.createMap().apply {
                    putBoolean("ready", true)
                })
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HID_DEVICE) {
                hidDevice = null
                sendEvent("onHidReady", Arguments.createMap().apply {
                    putBoolean("ready", false)
                })
            }
        }
    }

    // Broadcast receiver to track discovered devices during scan
    private val discoveryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    device?.let {
                        sendEvent("onDeviceFound", Arguments.createMap().apply {
                            putString("address", it.address)
                            putString("name", it.name ?: "Unknown")
                        })
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    sendEvent("onScanFinished", Arguments.createMap())
                }
            }
        }
    }

    override fun getName() = NAME

    @ReactMethod
    fun initialize(promise: Promise) {
        if (bluetoothAdapter == null) {
            promise.reject("BT_UNAVAILABLE", "Bluetooth not available on this device")
            return
        }
        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            }
            reactApplicationContext.registerReceiver(discoveryReceiver, filter)
            bluetoothAdapter.getProfileProxy(
                reactApplicationContext,
                profileListener,
                BluetoothProfile.HID_DEVICE
            )
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", e.message)
        }
    }

    @ReactMethod
    fun isBluetoothEnabled(promise: Promise) {
        promise.resolve(bluetoothAdapter?.isEnabled == true)
    }

    @ReactMethod
    fun startScan(promise: Promise) {
        try {
            bluetoothAdapter?.startDiscovery()
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SCAN_ERROR", e.message)
        }
    }

    @ReactMethod
    fun stopScan(promise: Promise) {
        bluetoothAdapter?.cancelDiscovery()
        promise.resolve(true)
    }

    @ReactMethod
    fun getPairedDevices(promise: Promise) {
        try {
            val paired = bluetoothAdapter?.bondedDevices ?: emptySet()
            val result = Arguments.createArray()
            for (device in paired) {
                result.pushMap(Arguments.createMap().apply {
                    putString("address", device.address)
                    putString("name", device.name ?: "Unknown")
                })
            }
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("PAIRED_ERROR", e.message)
        }
    }

    @ReactMethod
    fun connectDevice(address: String, promise: Promise) {
        try {
            val device = bluetoothAdapter?.getRemoteDevice(address)
            if (device == null) {
                promise.reject("DEVICE_NOT_FOUND", "Device not found: $address")
                return
            }
            val result = hidDevice?.connect(device) ?: false
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("CONNECT_ERROR", e.message)
        }
    }

    @ReactMethod
    fun disconnectDevice(promise: Promise) {
        try {
            connectedDevice?.let { hidDevice?.disconnect(it) }
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("DISCONNECT_ERROR", e.message)
        }
    }

    @ReactMethod
    fun sendKey(modifier: Int, keyCode: Int, promise: Promise) {
        val device = connectedDevice
        val hid = hidDevice
        if (device == null || hid == null) {
            promise.reject("NOT_CONNECTED", "No device connected")
            return
        }
        try {
            // Key press
            val pressReport = byteArrayOf(
                modifier.toByte(), 0x00,
                keyCode.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00
            )
            hid.sendReport(device, 0, pressReport)
            // Key release
            val releaseReport = ByteArray(8)
            hid.sendReport(device, 0, releaseReport)
            promise.resolve(true)
        } catch (e: Exception) {
            promise.reject("SEND_ERROR", e.message)
        }
    }

    @ReactMethod
    fun sendText(text: String, delayMs: Int, promise: Promise) {
        val device = connectedDevice
        val hid = hidDevice
        if (device == null || hid == null) {
            promise.reject("NOT_CONNECTED", "No device connected")
            return
        }
        autoTypeThread?.interrupt()
        autoTypeThread = Thread {
            try {
                for (char in text) {
                    if (Thread.currentThread().isInterrupted) break
                    val mapping = ASCII_TO_KEYCODE[char]
                    if (mapping != null) {
                        val (modifier, keyCode) = mapping
                        val pressReport = byteArrayOf(
                            modifier, 0x00,
                            keyCode, 0x00, 0x00, 0x00, 0x00, 0x00
                        )
                        hid.sendReport(device, 0, pressReport)
                        Thread.sleep(20)
                        hid.sendReport(device, 0, ByteArray(8))
                        if (delayMs > 0) Thread.sleep(delayMs.toLong())
                    }
                }
                promise.resolve(true)
            } catch (e: InterruptedException) {
                promise.resolve(false)
            } catch (e: Exception) {
                promise.reject("SEND_TEXT_ERROR", e.message)
            }
        }
        autoTypeThread!!.start()
    }

    @ReactMethod
    fun stopAutoType(promise: Promise) {
        autoTypeThread?.interrupt()
        promise.resolve(true)
    }

    @ReactMethod
    fun addListener(eventName: String) {}

    @ReactMethod
    fun removeListeners(count: Int) {}

    private fun sendEvent(eventName: String, params: WritableMap) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    override fun invalidate() {
        try {
            reactApplicationContext.unregisterReceiver(discoveryReceiver)
        } catch (_: Exception) {}
        hidDevice?.unregisterApp()
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        super.invalidate()
    }
}
