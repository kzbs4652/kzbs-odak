package com.kzbs.odak.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.kzbs.odak.data.FocusPreferences

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val preferences = FocusPreferences(context)
            // If phone was restarted during an active focus session and time remains:
            if (preferences.isFocusActive && preferences.getRemainingMillis() > 0) {
                // Restart focus foreground service immediately!
                FocusForegroundService.startService(context)
            } else {
                preferences.isFocusActive = false
            }
        }
    }
}
