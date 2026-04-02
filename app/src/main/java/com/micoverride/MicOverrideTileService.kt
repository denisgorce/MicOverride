package com.micoverride

import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi

/**
 * Quick Settings Tile — appears in the notification shade pulldown.
 * One tap activates the system-wide mic override BEFORE opening
 * Google Translate / Microsoft Translator.
 *
 * Requires Android 7.0+ (API 24)
 */
@RequiresApi(Build.VERSION_CODES.N)
class MicOverrideTileService : TileService() {

    private lateinit var audioManager: AudioManager
    private var isActive = false

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
    }

    override fun onStartListening() {
        super.onStartListening()
        // Sync tile state with current audio mode
        isActive = (audioManager.mode == AudioManager.MODE_IN_COMMUNICATION)
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        if (isActive) {
            deactivate()
        } else {
            activate()
        }
        updateTile()
    }

    private fun activate() {
        isActive = true

        // 1. Switch to IN_COMMUNICATION mode — this is the most effective
        //    system-wide signal to prefer the built-in bottom mic over headset.
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

        // 2. On some devices/ROMs, these low-level HAL parameters help.
        //    They are silently ignored on unsupported devices — safe to call.
        try {
            audioManager.setParameters("input_source=BUILTIN_MIC")
            audioManager.setParameters("audio_input_device=builtin_mic")
        } catch (_: Exception) {}

        // 3. On Android 12+: try to set a global preferred input device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            val builtinMic = devices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC
            }
            // setCommunicationDevice hints the system to prefer this device
            // for communication streams — affects Google Translate, etc.
            if (builtinMic != null) {
                audioManager.setCommunicationDevice(builtinMic)
            }
        }
    }

    private fun deactivate() {
        isActive = false
        audioManager.mode = AudioManager.MODE_NORMAL
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
        try {
            audioManager.setParameters("input_source=DEFAULT")
        } catch (_: Exception) {}
    }

    private fun updateTile() {
        qsTile?.apply {
            state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = if (isActive) "Micro intégré" else "Micro intégré"
            subtitle = if (isActive) "Forcé ✓" else "Inactif"
            contentDescription = if (isActive)
                "Override actif — micro intégré forcé"
            else
                "Appuyer pour forcer le micro intégré"
            updateTile()
        }
    }

    override fun onStopListening() {
        super.onStopListening()
    }
}
