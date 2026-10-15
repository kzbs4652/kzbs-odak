package com.kzbs.odak.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.kzbs.odak.data.FocusPreferences
import com.kzbs.odak.ui.OverlayLockActivity

class FocusAccessibilityService : AccessibilityService() {

    private lateinit var preferences: FocusPreferences
    private var lastBlockedPackage: String? = null
    private var lastBlockedTime: Long = 0
    private val handler = Handler(Looper.getMainLooper())

    // Known system/launcher packages that should never be blocked
    private val systemPackages = setOf(
        "com.android.systemui",
        "com.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.sec.android.app.launcher",           // Samsung default launcher
        "com.samsung.android.launcher",            // Samsung One UI launcher
        "com.samsung.android.lool",                // Samsung Device Care
        "com.samsung.android.app.routines",        // Samsung Routines
        "com.samsung.android.incallui",            // Samsung Phone UI
        "com.samsung.android.dialer",              // Samsung Dialer
        "com.google.android.dialer",               // Google Dialer
        "com.android.phone",                       // Phone
        "com.samsung.android.honeyboard",          // Samsung Keyboard
        "com.google.android.inputmethod.latin",    // Gboard
        "com.android.emergency",                   // Emergency
        "android"                                  // Android System
    )

    override fun onCreate() {
        super.onCreate()
        preferences = FocusPreferences(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Never block our own app
            if (packageName == applicationContext.packageName) {
                lastBlockedPackage = null
                return
            }

            // System packages and launchers
            if (isSystemPackage(packageName)) {
                // If user returned to home/launcher, reset last blocked package
                lastBlockedPackage = null
                return
            }

            // Check if focus session is active and if this app is blocked
            if (preferences.isAppBlocked(packageName)) {
                val now = System.currentTimeMillis()

                // Small 300ms debounce to prevent event flooding
                if (packageName == lastBlockedPackage && (now - lastBlockedTime) < 300) {
                    return
                }

                lastBlockedPackage = packageName
                lastBlockedTime = now
                preferences.incrementBlockedAttempts()

                // Step 1: Force exit target app immediately to Home screen
                performGlobalAction(GLOBAL_ACTION_HOME)

                // Step 2: Open our Lock Overlay Activity immediately on top of Home
                handler.postDelayed({
                    try {
                        val lockIntent = Intent(applicationContext, OverlayLockActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                    Intent.FLAG_ACTIVITY_NO_ANIMATION
                            putExtra(OverlayLockActivity.EXTRA_PACKAGE_NAME, packageName)
                        }
                        startActivity(lockIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 80)
            }
        }
    }

    private fun isSystemPackage(packageName: String): Boolean {
        if (systemPackages.contains(packageName)) return true
        if (packageName.contains("launcher") ||
            packageName.contains("home") ||
            packageName.contains("setupwizard")) {
            return true
        }
        return false
    }

    override fun onInterrupt() {
        // Required method
    }
}
