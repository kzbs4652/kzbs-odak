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

    // Samsung One UI specific: known system/launcher packages
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
        "android"                                  // System
    )

    override fun onCreate() {
        super.onCreate()
        preferences = FocusPreferences(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // Listen to both window state changes and window content changes for better detection
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return

            // Never block our own app
            if (packageName == applicationContext.packageName) {
                return
            }

            // Never block system packages, launchers, or essential apps
            if (isSystemPackage(packageName)) {
                return
            }

            // Check if focus is active and if app is in blocked list
            if (preferences.isAppBlocked(packageName)) {
                val now = System.currentTimeMillis()

                // Reduced debounce to 500ms for faster response (was 1200ms)
                // Samsung One UI needs faster reaction time
                if (packageName == lastBlockedPackage && (now - lastBlockedTime) < 500) {
                    return
                }

                lastBlockedPackage = packageName
                lastBlockedTime = now
                preferences.incrementBlockedAttempts()

                // CRITICAL FIX: Launch lock screen DIRECTLY on top of the blocked app
                // Do NOT call performGlobalAction(GLOBAL_ACTION_HOME) first!
                // On Samsung One UI, HOME + then Activity causes race condition
                // Instead, launch the lock activity immediately to cover the blocked app
                launchLockScreen(packageName)
            }
        }
    }

    private fun launchLockScreen(packageName: String) {
        val lockIntent = Intent(this, OverlayLockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            putExtra(OverlayLockActivity.EXTRA_PACKAGE_NAME, packageName)
        }
        startActivity(lockIntent)

        // Samsung One UI: Also send HOME action after a tiny delay as backup
        // This ensures the blocked app is fully pushed to background
        handler.postDelayed({
            performGlobalAction(GLOBAL_ACTION_HOME)
            // Re-launch lock screen after HOME to ensure it's visible
            handler.postDelayed({
                try {
                    val reLockIntent = Intent(this, OverlayLockActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                        putExtra(OverlayLockActivity.EXTRA_PACKAGE_NAME, packageName)
                    }
                    startActivity(reLockIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, 150)
        }, 100)
    }

    private fun isSystemPackage(packageName: String): Boolean {
        if (systemPackages.contains(packageName)) return true
        // Generic launcher/home detection
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

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Service connected - accessibility is active
    }
}
