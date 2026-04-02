package com.micoverride

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/**
 * Automatically restarts the MicOverride service after device reboot,
 * so the override is always active without manual intervention.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, MicOverrideService::class.java)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
