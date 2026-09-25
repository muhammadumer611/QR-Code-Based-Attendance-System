package com.university.attendance

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import java.security.SecureRandom
import java.util.UUID

/**
 * Handles the classroom proximity layer for attendance.
 *
 * The teacher phone advertises a short-lived, session-specific token.
 * Student phones scan for that token and only enable attendance when
 * the same token is detected repeatedly with an acceptable RSSI.
 *
 * BLE/RSSI is an approximate proximity signal, not an exact physical
 * boundary. The threshold is intentionally centralized here so it can
 * be tuned after real classroom testing without changing attendance logic.
 */
class BleAttendanceManager(
    context: Context
) {

    companion object {
        /** Private application UUID used for attendance advertisements. */
        /*
         * 16-bit app-specific UUID keeps the legacy BLE advertisement
         * comfortably inside the 31-byte payload limit while the
         * 128-bit random session token carries the per-session identity.
         */
        val SERVICE_UUID: UUID =
            UUID.fromString("0000F0AB-0000-1000-8000-00805F9B34FB")

        /** Approximate proximity threshold. Tune after testing on real phones. */
        const val MIN_RSSI_DBM = -70

        /** Number of good detections required before proximity is accepted. */
        const val REQUIRED_DETECTIONS = 3

        /** Detections older than this are discarded. */
        const val DETECTION_WINDOW_MS = 5_000L

        /** Generate a fresh 128-bit token for every attendance session. */
        fun generateSessionToken(): String {
            val bytes = ByteArray(16)
            SecureRandom().nextBytes(bytes)
            return bytes.joinToString("") { byte ->
                "%02x".format(byte.toInt() and 0xff)
            }
        }

        fun scanPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            }
        }

        fun advertisePermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            } else {
                emptyArray()
            }
        }
    }

    private val appContext = context.applicationContext

    private val bluetoothManager =
        appContext.getSystemService(
            BluetoothManager::class.java
        )

    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private var advertiser: BluetoothLeAdvertiser? = null
    private var advertiseCallback: AdvertiseCallback? = null

    private var scanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null

    private val handler =
        Handler(Looper.getMainLooper())

    private var targetTokens: Set<String> = emptySet()

    private val detections =
        mutableMapOf<String, MutableList<Long>>()

    private var nearbyTokens: Set<String> = emptySet()

    private var proximityListener:
            ((Set<String>) -> Unit)? = null

    private val watchdog = object : Runnable {
        override fun run() {
            publishNearbyTokens()
            if (scanCallback != null) {
                handler.postDelayed(
                    this,
                    1_000L
                )
            }
        }
    }

    fun isBluetoothSupported(): Boolean =
        bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean =
        try {
            bluetoothAdapter?.isEnabled == true
        } catch (_: SecurityException) {
            false
        }

    fun hasScanPermissions(): Boolean =
        scanPermissions().all {
            ContextCompat.checkSelfPermission(
                appContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    fun hasAdvertisePermissions(): Boolean =
        advertisePermissions().all {
            ContextCompat.checkSelfPermission(
                appContext,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

    /**
     * Starts advertising the session token from the teacher phone.
     */
    fun startAdvertising(
        token: String,
        onStarted: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (token.isBlank()) {
            onError("BLE attendance token is missing.")
            return
        }

        if (!isBluetoothSupported()) {
            onError("This phone does not support Bluetooth.")
            return
        }

        if (!isBluetoothEnabled()) {
            onError("Bluetooth is turned off.")
            return
        }

        if (!hasAdvertisePermissions()) {
            onError("Bluetooth advertising permission is not granted.")
            return
        }

        stopAdvertising()

        val adapter = bluetoothAdapter
        val bleAdvertiser = try {
            adapter?.bluetoothLeAdvertiser
        } catch (_: SecurityException) {
            null
        }

        if (bleAdvertiser == null) {
            onError(
                "BLE advertising is not available on this phone."
            )
            return
        }

        val tokenBytes = tokenToBytes(token)
            ?: run {
                onError("Invalid BLE attendance token.")
                return
            }

        val serviceUuid =
            ParcelUuid(SERVICE_UUID)

        val settings =
            AdvertiseSettings.Builder()
                .setAdvertiseMode(
                    AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY
                )
                .setTxPowerLevel(
                    AdvertiseSettings.ADVERTISE_TX_POWER_HIGH
                )
                .setConnectable(false)
                .setTimeout(0)
                .build()

        val data =
            AdvertiseData.Builder()
                .addServiceUuid(serviceUuid)
                .addServiceData(
                    serviceUuid,
                    tokenBytes
                )
                .setIncludeDeviceName(false)
                .build()

        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(
                settingsInEffect: AdvertiseSettings?
            ) {
                onStarted()
            }

            override fun onStartFailure(
                errorCode: Int
            ) {
                onError(
                    advertiseErrorMessage(errorCode)
                )
            }
        }

        advertiser = bleAdvertiser
        advertiseCallback = callback

        try {
            bleAdvertiser.startAdvertising(
                settings,
                data,
                callback
            )
        } catch (e: SecurityException) {
            advertiser = null
            advertiseCallback = null
            onError(
                e.message
                    ?: "Bluetooth advertising permission was denied."
            )
        } catch (e: Exception) {
            advertiser = null
            advertiseCallback = null
            onError(
                e.message
                    ?: "Unable to start Bluetooth advertising."
            )
        }
    }

    fun stopAdvertising() {
        val bleAdvertiser = advertiser
        val callback = advertiseCallback

        if (bleAdvertiser != null && callback != null) {
            try {
                bleAdvertiser.stopAdvertising(callback)
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }

        advertiser = null
        advertiseCallback = null
    }

    /**
     * Starts scanning for any currently active session token.
     * The callback contains only tokens that satisfy the RSSI + repeated
     * detection rule. It becomes empty when the signal disappears.
     */
    fun startScanning(
        sessionTokens: Set<String>,
        onNearbyTokensChanged: (Set<String>) -> Unit,
        onError: (String) -> Unit
    ) {
        stopScanning()

        if (!isBluetoothSupported()) {
            onError("This phone does not support Bluetooth.")
            return
        }

        if (!isBluetoothEnabled()) {
            onError("Bluetooth is turned off.")
            return
        }

        if (!hasScanPermissions()) {
            onError("Bluetooth scanning permission is not granted.")
            return
        }

        targetTokens =
            sessionTokens
                .filter { it.isNotBlank() }
                .toSet()

        proximityListener =
            onNearbyTokensChanged

        val bleScanner = try {
            bluetoothAdapter?.bluetoothLeScanner
        } catch (_: SecurityException) {
            null
        }

        if (bleScanner == null) {
            onError(
                "BLE scanning is not available on this phone."
            )
            return
        }

        val serviceUuid =
            ParcelUuid(SERVICE_UUID)

        val filter =
            ScanFilter.Builder()
                .setServiceUuid(serviceUuid)
                .build()

        val settings =
            ScanSettings.Builder()
                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {
                handleScanResult(result)
            }

            override fun onBatchScanResults(
                results: MutableList<ScanResult>
            ) {
                results.forEach(::handleScanResult)
            }

            override fun onScanFailed(
                errorCode: Int
            ) {
                onError(
                    scanErrorMessage(errorCode)
                )
            }
        }

        scanner = bleScanner
        scanCallback = callback

        try {
            bleScanner.startScan(
                listOf(filter),
                settings,
                callback
            )
            handler.post(watchdog)
        } catch (e: SecurityException) {
            scanner = null
            scanCallback = null
            onError(
                e.message
                    ?: "Bluetooth scanning permission was denied."
            )
        } catch (e: Exception) {
            scanner = null
            scanCallback = null
            onError(
                e.message
                    ?: "Unable to start Bluetooth scanning."
            )
        }
    }

    fun updateTargetTokens(
        sessionTokens: Set<String>
    ) {
        targetTokens =
            sessionTokens
                .filter { it.isNotBlank() }
                .toSet()

        detections.keys
            .toList()
            .filter { it !in targetTokens }
            .forEach {
                detections.remove(it)
            }

        publishNearbyTokens()
    }

    fun stopScanning() {
        handler.removeCallbacks(watchdog)

        val bleScanner = scanner
        val callback = scanCallback

        if (bleScanner != null && callback != null) {
            try {
                bleScanner.stopScan(callback)
            } catch (_: SecurityException) {
            } catch (_: Exception) {
            }
        }

        scanner = null
        scanCallback = null
        targetTokens = emptySet()
        detections.clear()
        nearbyTokens = emptySet()
        proximityListener = null
    }

    private fun handleScanResult(
        result: ScanResult
    ) {
        val record = result.scanRecord
            ?: return

        val serviceUuid =
            ParcelUuid(SERVICE_UUID)

        val bytes = try {
            record.getServiceData(serviceUuid)
        } catch (_: Exception) {
            null
        } ?: return

        val token = bytesToHex(bytes)

        if (token !in targetTokens) {
            return
        }

        val rssi = result.rssi

        if (rssi < MIN_RSSI_DBM) {
            return
        }

        val now = System.currentTimeMillis()
        val history =
            detections.getOrPut(token) {
                mutableListOf()
            }

        history.add(now)

        val cutoff =
            now - DETECTION_WINDOW_MS

        history.removeAll { it < cutoff }

        publishNearbyTokens()
    }

    private fun publishNearbyTokens(
        force: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        val cutoff =
            now - DETECTION_WINDOW_MS

        detections.values.forEach { history ->
            history.removeAll { it < cutoff }
        }

        val updated =
            detections
                .filterKeys { it in targetTokens }
                .filterValues {
                    it.size >= REQUIRED_DETECTIONS
                }
                .keys
                .toSet()

        if (force || updated != nearbyTokens) {
            nearbyTokens = updated
            proximityListener?.invoke(
                nearbyTokens
            )
        }
    }

    private fun tokenToBytes(
        token: String
    ): ByteArray? {
        if (token.length != 32) {
            return null
        }

        return try {
            ByteArray(16) { index ->
                token
                    .substring(
                        index * 2,
                        index * 2 + 2
                    )
                    .toInt(16)
                    .toByte()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun bytesToHex(
        bytes: ByteArray
    ): String =
        bytes.joinToString("") { byte ->
            "%02x".format(byte.toInt() and 0xff)
        }

    private fun advertiseErrorMessage(
        errorCode: Int
    ): String = when (errorCode) {
        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED ->
            "Bluetooth attendance is already being advertised."
        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE ->
            "BLE attendance data is too large for this phone."
        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED ->
            "This phone does not support BLE advertising."
        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR ->
            "Bluetooth reported an internal advertising error."
        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS ->
            "This phone has no free BLE advertiser slot."
        else ->
            "Unable to start BLE attendance advertising (code $errorCode)."
    }

    private fun scanErrorMessage(
        errorCode: Int
    ): String = when (errorCode) {
        ScanCallback.SCAN_FAILED_ALREADY_STARTED ->
            "Bluetooth attendance scanning is already running."
        ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED ->
            "Bluetooth scanner could not be registered."
        ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED ->
            "This phone does not support BLE scanning."
        ScanCallback.SCAN_FAILED_INTERNAL_ERROR ->
            "Bluetooth reported an internal scanning error."
        else ->
            "Unable to scan for classroom attendance (code $errorCode)."
    }
}
