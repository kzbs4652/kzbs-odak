package com.kzbs.odak.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.kzbs.odak.KZBSApplication
import com.kzbs.odak.R
import com.kzbs.odak.data.FocusPreferences
import com.kzbs.odak.ui.MainActivity
import com.kzbs.odak.ui.OverlayLockActivity
import java.util.Locale
import java.util.concurrent.TimeUnit

class FocusForegroundService : Service() {

    private lateinit var preferences: FocusPreferences
    private val handler = Handler(Looper.getMainLooper())
    private val updateInterval = 1000L
    private var lastCheckedPkg: String? = null

    companion object {
        const val ACTION_START = "ACTION_START_FOCUS"
        const val ACTION_STOP = "ACTION_STOP_FOCUS"
        const val NOTIFICATION_ID = 1001

        fun startService(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                action = ACTION_START
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, FocusForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val countdownRunnable = object : Runnable {
        override fun run() {
            // Check break expiration
            preferences.checkBreakExpired()

            val remainingMillis = preferences.getRemainingMillis()
            if (remainingMillis <= 0L) {
                preferences.endFocusSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            } else {
                updateNotification(remainingMillis)
                // Secondary fallback protection: Check foreground app via UsageStats
                checkForegroundBlockedApp()
                handler.postDelayed(this, updateInterval)
            }
        }
    }

    private fun checkForegroundBlockedApp() {
        if (!preferences.isFocusActive) return
        try {
            val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
            val now = System.currentTimeMillis()
            val events = usageStatsManager.queryEvents(now - 3000, now)
            val event = UsageEvents.Event()
            var currentPkg: String? = null

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    currentPkg = event.packageName
                }
            }

            if (currentPkg != null && currentPkg != packageName && currentPkg != "com.android.systemui") {
                if (preferences.isAppBlocked(currentPkg) && currentPkg != lastCheckedPkg) {
                    lastCheckedPkg = currentPkg
                    preferences.incrementBlockedAttempts()

                    // Send home
                    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_HOME)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(homeIntent)

                    // Show lock overlay
                    handler.postDelayed({
                        val lockIntent = Intent(applicationContext, OverlayLockActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                            putExtra(OverlayLockActivity.EXTRA_PACKAGE_NAME, currentPkg)
                        }
                        startActivity(lockIntent)
                    }, 100)
                } else if (!preferences.isAppBlocked(currentPkg)) {
                    lastCheckedPkg = null
                }
            }
        } catch (e: Exception) {
            // Usage stats might not be granted
        }
    }

    override fun onCreate() {
        super.onCreate()
        preferences = FocusPreferences(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                handler.removeCallbacks(countdownRunnable)
                preferences.endFocusSession()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START, null -> {
                startForeground(NOTIFICATION_ID, buildNotification("Odak başlatılıyor..."))
                handler.removeCallbacks(countdownRunnable)
                handler.post(countdownRunnable)
            }
        }
        return START_STICKY
    }

    private fun updateNotification(remainingMillis: Long) {
        val hours = TimeUnit.MILLISECONDS.toHours(remainingMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remainingMillis) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remainingMillis) % 60

        val timeStr = if (hours > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }

        val statusText = if (preferences.isBreakActive) {
            "☕ Mola Aktif • Odak Kalan: $timeStr"
        } else {
            "Kalan Süre: $timeStr • Odak Modu Aktif"
        }

        val notification = buildNotification(statusText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, KZBSApplication.FOCUS_CHANNEL_ID)
            .setContentTitle("KZBS ODAK KORUMASI")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_kzbs_logo)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(countdownRunnable)
        super.onDestroy()
    }
}
