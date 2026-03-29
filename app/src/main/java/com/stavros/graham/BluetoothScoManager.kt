package com.stavros.graham

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

private const val TAG = "BluetoothScoManager"

class BluetoothScoManager(private val context: Context) {
    private var isActive = false

    // Cached after the first successful findBluetoothDevice() call. The BLE link persists
    // across start/stop cycles within a conversation, so re-using the same AudioDeviceInfo
    // avoids re-enumerating devices on every Listening entry.
    private var cachedDevice: AudioDeviceInfo? = null

    // The input-side AudioDeviceInfo corresponding to cachedDevice, for use as
    // AudioRecord.setPreferredDevice(). Exposed so ConversationScreen can assign it to
    // SpeechRecognizer.preferredInputDevice before startListening().
    var preferredInputDevice: AudioDeviceInfo? = null
        private set

    // Kept for legacy API < 31 SCO state tracking; harmless on API 31+.
    private val scoStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val state = intent.getIntExtra(
                AudioManager.EXTRA_SCO_AUDIO_STATE,
                AudioManager.SCO_AUDIO_STATE_ERROR,
            )
            Log.d(TAG, "SCO audio state changed: $state")
        }
    }

    init {
        context.registerReceiver(
            scoStateReceiver,
            IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED),
        )
    }

    // Sets the communication device so AudioRecord routes through the BT mic. Idempotent
    // while already active. The device is cached after the first lookup so that subsequent
    // start() calls (after stop() cleared isActive) skip enumeration and call
    // setCommunicationDevice directly — measuring whether the re-set is fast when the BLE
    // link is already established.
    fun start(audioManager: AudioManager) {
        if (isActive) {
            Log.d(TAG, "Already active; skipping start")
            return
        }
        if (!hasBluetoothConnectPermission()) {
            Log.d(TAG, "BLUETOOTH_CONNECT not granted; skipping BT mic setup")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (cachedDevice == null) {
                cachedDevice = findBluetoothDevice(audioManager)
            }
            val device = cachedDevice ?: run {
                Log.d(TAG, "No BT communication device found; using default mic")
                return
            }
            val before = System.currentTimeMillis()
            val success = audioManager.setCommunicationDevice(device)
            val elapsed = System.currentTimeMillis() - before
            Log.d(TAG, "setCommunicationDevice(${device.productName}, type=${device.type}) -> $success (${elapsed}ms)")
            if (!success) {
                Log.d(TAG, "setCommunicationDevice failed; invalidating cache and retrying")
                cachedDevice = null
                cachedDevice = findBluetoothDevice(audioManager)
                val retryDevice = cachedDevice ?: return
                val retrySuccess = audioManager.setCommunicationDevice(retryDevice)
                Log.d(TAG, "setCommunicationDevice retry(${retryDevice.productName}, type=${retryDevice.type}) -> $retrySuccess")
                isActive = retrySuccess
            } else {
                isActive = true
            }
        } else {
            Log.d(TAG, "Starting Bluetooth SCO (legacy)")
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
            isActive = true
        }
    }

    fun stop(audioManager: AudioManager) {
        if (!isActive) {
            Log.d(TAG, "Not active; skipping stop")
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            Log.d(TAG, "Cleared communication device")
        } else {
            if (!hasBluetoothConnectPermission()) {
                Log.d(TAG, "BLUETOOTH_CONNECT not granted; skipping legacy SCO stop")
                isActive = false
                return
            }
            Log.d(TAG, "Stopping Bluetooth SCO (legacy)")
            @Suppress("DEPRECATION")
            audioManager.stopBluetoothSco()
        }
        // cachedDevice is intentionally kept so the next start() can skip enumeration.
        isActive = false
    }

    // Enumerates available communication devices, picks the first non-builtin one, and
    // caches the matching input device for AudioRecord.setPreferredDevice().
    private fun findBluetoothDevice(audioManager: AudioManager): AudioDeviceInfo? {
        val inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
        Log.d(TAG, "Input devices (${inputDevices.size} total):")
        for (device in inputDevices) {
            Log.d(TAG, "  id=${device.id}, type=${device.type}, name=${device.productName}")
        }

        val commDevices = audioManager.availableCommunicationDevices
        Log.d(TAG, "Available communication devices (${commDevices.size} total):")
        for (device in commDevices) {
            Log.d(TAG, "  id=${device.id}, type=${device.type}, name=${device.productName}, isSink=${device.isSink}")
        }

        val bluetoothCommTypes = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
        )
        val chosen = commDevices.firstOrNull { it.type in bluetoothCommTypes } ?: return null

        preferredInputDevice = inputDevices.firstOrNull { it.id == chosen.id }
            ?: inputDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }
        Log.d(TAG, "Chose communication device: ${chosen.productName} type=${chosen.type}, preferredInput=${preferredInputDevice?.productName}")
        return chosen
    }

    // BLUETOOTH_CONNECT is only a runtime permission from API 31 onward; on older versions
    // the manifest declaration alone is sufficient and the call is always safe.
    private fun hasBluetoothConnectPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun destroy() {
        Log.d(TAG, "Destroying BluetoothScoManager")
        cachedDevice = null
        preferredInputDevice = null
        context.unregisterReceiver(scoStateReceiver)
    }
}
