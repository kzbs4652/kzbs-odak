package com.kzbs.odak.service

import android.app.Notification
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.kzbs.odak.data.FocusPreferences

class NotificationBlockerService : NotificationListenerService() {

    private lateinit var preferences: FocusPreferences

    override fun onCreate() {
        super.onCreate()
        preferences = FocusPreferences(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return

        // Check if focus session is active and notifications from this app should be muted
        if (preferences.isNotificationBlocked(packageName)) {
            try {
                // Cancel/discard incoming notification immediately
                cancelNotification(sbn.key)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Samsung One UI: Also try to snooze the notification if cancel fails
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    snoozeNotification(sbn.key, 24 * 60 * 60 * 1000L) // Snooze for 24h as backup
                }
            } catch (e: Exception) {
                // Snooze not supported or failed — that's ok, cancel should have worked
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        // When the listener connects, clear any existing notifications from blocked apps
        clearExistingBlockedNotifications()
    }

    private fun clearExistingBlockedNotifications() {
        try {
            val activeNotifications = getActiveNotifications() ?: return
            for (sbn in activeNotifications) {
                val packageName = sbn.packageName ?: continue
                if (preferences.isNotificationBlocked(packageName)) {
                    try {
                        cancelNotification(sbn.key)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
