package com.conkey

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHidDevice
import android.bluetooth.BluetoothHidDeviceAppSdpSettings
import android.bluetooth.BluetoothHidDeviceAppQosSettings
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
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

    // True once the HID app has finished registering with the BT stack.
    @Volatile private var appRegistered = false
    // A device the user asked to connect to before registration completed.
    @Volatile private var pendingConnect: BluetoothDevice? = null
    // Explicit stop flag — more reliable than Thread.isInterrupted() because
    // Thread.sleep() inside flow-control helpers clears the interrupt flag.
    @Volatile private var stopTyping = false
    // Deterministic RNG seeded lazily; varied per-character for human-like cadence.
    private var rngState: Long = 0x2545F4914F6CDD1DL

    // Prevents CPU from sleeping while auto-type is running in the background.
    private var wakeLock: PowerManager.WakeLock? = null
    // Guard against registering the discoveryReceiver more than once.
    private var receiverRegistered = false

    private val hidCallback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(pluggedDevice: BluetoothDevice?, registered: Boolean) {
            appRegistered = registered
            sendEvent("onHidAppStatus", Arguments.createMap().apply {
                putBoolean("registered", registered)
                putString("device", pluggedDevice?.address)
            })
            // If the user tapped a device before registration finished, connect now.
            if (registered) {
                pendingConnect?.let { device ->
                    pendingConnect = null
                    hidDevice?.connect(device)
                }
            }
        }

        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice = device
                startForegroundService("Connected to ${device.name ?: device.address}")
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                if (connectedDevice?.address == device.address) connectedDevice = null
                stopForegroundService()
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
                // Don't re-register if the app is already registered with the
                // stack — registerApp() twice fails / duplicates the SDP record.
                if (appRegistered) {
                    sendEvent("onHidReady", Arguments.createMap().apply {
                        putBoolean("ready", true)
                    })
                    return
                }
                val sdpSettings = BluetoothHidDeviceAppSdpSettings(
                    "ConKey",
                    "Bluetooth Keyboard",
                    "ConKey App",
                    BluetoothHidDevice.SUBCLASS1_KEYBOARD,
                    KEYBOARD_DESCRIPTOR
                )
                // Best-effort QoS makes the stack tolerant of report bursts
                // instead of dropping the link when the buffer briefly fills.
                val qos = BluetoothHidDeviceAppQosSettings(
                    BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                    800,   // token rate
                    9,     // token bucket size
                    0,     // peak bandwidth
                    11250, // latency (µs)
                    BluetoothHidDeviceAppQosSettings.MAX
                )
                hidDevice?.registerApp(sdpSettings, null, qos, executor, hidCallback)
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
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val bondDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val bondState = intent.getIntExtra(
                        BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE
                    )
                    if (bondState == BluetoothDevice.BOND_BONDED) {
                        // Pairing finished — connect now if this is the device we wanted,
                        // and the HID app is ready (otherwise onAppStatusChanged handles it).
                        val pending = pendingConnect
                        if (pending != null && appRegistered && hidDevice != null) {
                            pendingConnect = null
                            hidDevice?.connect(pending)
                        }
                    } else if (bondState == BluetoothDevice.BOND_NONE) {
                        // Only treat this as a disconnect if it's the device we're
                        // actually connected to — unpairing some *other* device must
                        // not eject the user from an active keyboard session.
                        if (bondDevice != null && connectedDevice?.address == bondDevice.address) {
                            connectedDevice = null
                            stopForegroundService()
                            sendEvent("onConnectionStateChanged", Arguments.createMap().apply {
                                putString("address", bondDevice.address)
                                putString("name", bondDevice.name ?: "Unknown")
                                putInt("state", BluetoothProfile.STATE_DISCONNECTED)
                            })
                        }
                    }
                }
            }
        }
    }

    override fun getName() = NAME

    /**
     * Returns the device we can actually send to right now.
     *
     * The cached [connectedDevice] can go stale: a transient STATE_DISCONNECTED
     * nulls it, but the host often stays connected (or silently reconnects) at
     * the HID level without a fresh STATE_CONNECTED arriving. So we fall back to
     * the authoritative source — hidDevice.getConnectedDevices() — and re-cache
     * whatever it reports. This is why "no device connected" used to appear even
     * though the link was alive and reopening the app fixed it.
     */
    private fun resolveConnectedDevice(): BluetoothDevice? {
        val cached = connectedDevice
        if (cached != null) return cached
        val live = hidDevice?.connectedDevices?.firstOrNull()
        if (live != null) connectedDevice = live
        return live
    }

    @ReactMethod
    fun initialize(promise: Promise) {
        if (bluetoothAdapter == null) {
            promise.reject("BT_UNAVAILABLE", "Bluetooth not available on this device")
            return
        }
        try {
            if (!receiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                    addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                }
                reactApplicationContext.registerReceiver(discoveryReceiver, filter)
                receiverRegistered = true
            }
            // If the proxy is already up (screen remounted, e.g. user navigated
            // back), don't fetch a new one — that re-runs onServiceConnected and
            // double-registers the HID app, which the stack rejects. Just
            // re-emit current readiness so the UI's status dot is correct.
            if (hidDevice != null) {
                sendEvent("onHidReady", Arguments.createMap().apply {
                    putBoolean("ready", true)
                })
                promise.resolve(true)
                return
            }
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
            val adapter = bluetoothAdapter
            if (adapter == null || !adapter.isEnabled) {
                promise.reject("SCAN_ERROR", "Bluetooth is not enabled")
                return
            }
            val started = adapter.startDiscovery()
            if (!started) {
                promise.reject("SCAN_ERROR", "startDiscovery() returned false — check permissions or that another scan is not already running")
                return
            }
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

            // Stop discovery first — scanning starves the connection attempt.
            bluetoothAdapter.cancelDiscovery()

            // Must be bonded (paired) before a HID connection can be established.
            // createBond() is async; the actual connect happens once bonded + registered.
            if (device.bondState != BluetoothDevice.BOND_BONDED) {
                pendingConnect = device
                val started = device.createBond()
                promise.resolve(started)
                return
            }

            // If the HID app isn't registered yet, defer the connect.
            if (!appRegistered || hidDevice == null) {
                pendingConnect = device
                promise.resolve(true)
                return
            }

            val result = hidDevice?.connect(device) ?: false
            promise.resolve(result)
        } catch (e: Exception) {
            promise.reject("CONNECT_ERROR", e.message)
        }
    }

    @ReactMethod
    fun isConnected(promise: Promise) {
        promise.resolve(resolveConnectedDevice() != null)
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
        val device = resolveConnectedDevice()
        val hid = hidDevice
        if (device == null || hid == null) {
            promise.reject("NOT_CONNECTED", "No device connected")
            return
        }
        // Run off the JS thread so the hold-time sleep doesn't block the bridge.
        executor.execute {
            try {
                pressAndRelease(hid, device, modifier.toByte(), keyCode.toByte())
                promise.resolve(true)
            } catch (e: Exception) {
                promise.reject("SEND_ERROR", e.message)
            }
        }
    }

    /**
     * Sends a single keystroke as a proper press → hold → release cycle.
     *
     * The host registers a key on the press report and stops on the release
     * (all-zero) report. If the release arrives too quickly the host may either
     * miss the press or trigger key auto-repeat, causing one tap to type twice.
     * A short, guaranteed hold (and a guaranteed release in finally) fixes that.
     */
    private fun pressAndRelease(
        hid: BluetoothHidDevice,
        device: BluetoothDevice,
        modifier: Byte,
        keyCode: Byte
    ) {
        val pressReport = byteArrayOf(
            modifier, 0x00, keyCode, 0x00, 0x00, 0x00, 0x00, 0x00
        )
        try {
            sendReportFlowControlled(hid, device, pressReport)
            // Hold long enough for the host to register exactly one key-down.
            Thread.sleep(12)
        } finally {
            // Always release, even if the hold was interrupted, so the key
            // never sticks down (which would auto-repeat forever).
            sendReportFlowControlled(hid, device, EMPTY_REPORT)
        }
    }

    // Reusable all-zero release report.
    private val EMPTY_REPORT = ByteArray(8)

    /**
     * Sends a HID report with simple flow control.
     *
     * sendReport() returns false when the radio's outgoing buffer is full.
     * Ignoring that and pushing more reports overflows the L2CAP channel, and
     * the Bluetooth stack then drops the whole connection to recover — which is
     * what caused the "auto-disconnect while typing" bug. Instead we back off
     * and retry a few times, giving the buffer time to drain.
     */
    private fun sendReportFlowControlled(
        hid: BluetoothHidDevice,
        device: BluetoothDevice,
        report: ByteArray
    ) {
        var attempt = 0
        while (attempt < 8) {
            if (stopTyping) return
            if (hid.sendReport(device, 0, report)) return
            try {
                Thread.sleep(5L + attempt * 3L)
            } catch (e: InterruptedException) {
                // Restore the interrupt flag so the outer loop sees it,
                // then bail — this is what was swallowing the stop signal.
                Thread.currentThread().interrupt()
                return
            }
            attempt++
        }
        hid.sendReport(device, 0, report)
    }

    /** xorshift64 RNG — avoids java.util.Random's allocation in the type loop. */
    private fun nextRandomFraction(): Double {
        // Lazily seed from the system clock so cadence differs each run.
        if (rngState == 0x2545F4914F6CDD1DL) {
            rngState = System.nanoTime() xor 0x2545F4914F6CDD1DL
        }
        var x = rngState
        x = x xor (x shl 13)
        x = x xor (x ushr 7)
        x = x xor (x shl 17)
        rngState = x
        // Map to [0, 1).
        return ((x ushr 11).toDouble()) / (1L shl 53).toDouble()
    }

    /**
     * Auto-types [text] keystroke-by-keystroke.
     *
     * @param delayMs       base inter-key delay in milliseconds, kept to 5-decimal
     *                      precision (e.g. 49.37512). Sub-millisecond fractions are
     *                      applied via nanosecond-precision sleeps.
     * @param randomness    0.0 = constant delay; 1.0 = each delay varies up to ±100%
     *                      of the base, giving a human-like irregular cadence.
     */
    @ReactMethod
    fun sendText(text: String, delayMs: Double, randomness: Double, promise: Promise) {
        val device = resolveConnectedDevice()
        val hid = hidDevice
        if (device == null || hid == null) {
            promise.reject("NOT_CONNECTED", "No device connected")
            return
        }
        stopTyping = false
        autoTypeThread?.interrupt()
        autoTypeThread = Thread {
            acquireWakeLock()
            try {
                val factor = randomness.coerceIn(0.0, 1.0)
                var sent = 0
                for (char in text) {
                    if (stopTyping || Thread.currentThread().isInterrupted) break
                    // Stop only if the host is genuinely gone — check the live
                    // connected list, not just the cache (which can be transiently
                    // null while the link is still up).
                    val stillConnected = connectedDevice?.address == device.address ||
                        hid.connectedDevices.any { it.address == device.address }
                    if (!stillConnected) {
                        sendEvent("onAutoTypeProgress", Arguments.createMap().apply {
                            putInt("sent", sent)
                            putInt("total", text.length)
                        })
                        promise.resolve(false)
                        return@Thread
                    }
                    val mapping = ASCII_TO_KEYCODE[char] ?: continue
                    val (modifier, keyCode) = mapping
                    pressAndRelease(hid, device, modifier, keyCode)
                    sent++

                    // Compute this character's delay with optional randomness.
                    // Round to 5 decimals so the configured precision is honored.
                    val jitter = if (factor > 0.0) {
                        // Symmetric variation in [-factor, +factor].
                        1.0 + (nextRandomFraction() * 2.0 - 1.0) * factor
                    } else 1.0
                    // Floor at 5 ms between characters: faster than this and the
                    // radio buffer can't drain, which drops the connection.
                    val effectiveMs = roundTo5(delayMs * jitter).coerceAtLeast(5.0)
                    sleepPrecise(effectiveMs)

                    // Periodic progress so the UI can show a live count.
                    if (sent % 8 == 0) {
                        sendEvent("onAutoTypeProgress", Arguments.createMap().apply {
                            putInt("sent", sent)
                            putInt("total", text.length)
                        })
                        updateForegroundNotification("Typing… $sent/${text.length}")
                    }
                }
                sendEvent("onAutoTypeProgress", Arguments.createMap().apply {
                    putInt("sent", sent)
                    putInt("total", text.length)
                })
                updateForegroundNotification("Connected to ${device.name ?: device.address}")
                promise.resolve(true)
            } catch (e: InterruptedException) {
                promise.resolve(false)
            } catch (e: Exception) {
                promise.reject("SEND_TEXT_ERROR", e.message)
            } finally {
                releaseWakeLock()
            }
        }
        autoTypeThread!!.start()
    }

    /** Rounds a millisecond value to 5 decimal places (0.00001 ms resolution). */
    private fun roundTo5(value: Double): Double {
        return Math.round(value * 100000.0) / 100000.0
    }

    /** Sleeps for [ms] milliseconds with nanosecond precision. Propagates interrupts. */
    private fun sleepPrecise(ms: Double) {
        if (ms <= 0.0) return
        val whole = ms.toLong()
        val nanos = ((ms - whole) * 1_000_000.0).toInt().coerceIn(0, 999_999)
        try {
            Thread.sleep(whole, nanos)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    @ReactMethod
    fun stopAutoType(promise: Promise) {
        stopTyping = true
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

    private fun startForegroundService(status: String) {
        val ctx = reactApplicationContext
        val intent = Intent(ctx, BluetoothHidService::class.java).apply {
            putExtra(BluetoothHidService.EXTRA_STATUS, status)
        }
        // Starting an FGS while the app is in the background throws
        // ForegroundServiceStartNotAllowedException on Android 12+. The BT
        // stack can fire connection callbacks at any time (e.g. host
        // auto-reconnects after we're backgrounded), so guard against it —
        // a missing background notification is far better than a crash.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        } catch (e: Exception) {
            // Best-effort: connection still works, we just couldn't post the
            // ongoing notification because we're not allowed to start an FGS now.
        }
    }

    private fun stopForegroundService() {
        try {
            reactApplicationContext.stopService(
                Intent(reactApplicationContext, BluetoothHidService::class.java)
            )
        } catch (_: Exception) {}
    }

    /**
     * Updates the ongoing notification's text WITHOUT (re)starting the service.
     * Called frequently during auto-type, so it must not hit FGS start paths.
     */
    private fun updateForegroundNotification(status: String) {
        BluetoothHidService.updateNotification(reactApplicationContext, status)
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = reactApplicationContext.getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "conkey:autotype"
        ).also { it.acquire(30 * 60 * 1000L) } // max 30 min
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
    }

    override fun invalidate() {
        releaseWakeLock()
        stopForegroundService()
        if (receiverRegistered) {
            try {
                reactApplicationContext.unregisterReceiver(discoveryReceiver)
            } catch (_: Exception) {}
            receiverRegistered = false
        }
        hidDevice?.unregisterApp()
        bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, hidDevice)
        super.invalidate()
    }
}
