package com.micoverride

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

/**
 * Foreground service that keeps the mic override active even
 * when the app is in the background.
 */
class MicOverrideService : Service() {

    private lateinit var audioManager: AudioManager
    private val CHANNEL_ID = "MicOverrideChannel"
    private val NOTIFICATION_ID = 1

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                if (state == 1) {
                    // Headset plugged in — force back to builtin mic
                    forceBuiltinMic()
                    updateNotification("🎙️ Kit détecté → Micro intégré forcé")
                } else if (state == 0) {
                    updateNotification("🎙️ Micro intégré actif (kit débranché)")
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()

        val filter = IntentFilter(AudioManager.ACTION_HEADSET_PLUG)
        registerReceiver(headsetReceiver, filter)

        startForeground(NOTIFICATION_ID, buildNotification("🎙️ Service actif — Micro intégré forcé"))
        forceBuiltinMic()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Restart service if killed by system
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(headsetReceiver) } catch (_: Exception) {}
    }

    private fun forceBuiltinMic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)
            devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                ?: return
            // The actual AudioRecord preferred device is set per-instance in MainActivity.
            // Here we handle system audio mode to deprioritize headset routing.
            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MicOverride Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Garde le micro intégré actif"
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MicOverride")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(text))
    }
}
