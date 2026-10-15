package com.kzbs.odak.data

import android.content.Context
import android.content.SharedPreferences

class FocusPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("kzbs_odak_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FIRST_RUN = "first_run_completed"
        private const val KEY_FOCUS_ACTIVE = "focus_active"
        private const val KEY_FOCUS_END_TIME = "focus_end_time"
        private const val KEY_FOCUS_DURATION_MINUTES = "focus_duration_minutes"
        private const val KEY_CUSTOM_LOCK_MESSAGE = "custom_lock_message"
        private const val KEY_BLOCKED_APPS = "blocked_apps"
        private const val KEY_BLOCKED_NOTIFICATIONS = "blocked_notifications"
        private const val KEY_SAVED_MINUTES = "saved_minutes"
        private const val KEY_BLOCKED_ATTEMPTS = "blocked_attempts"
        private const val KEY_HARDCORE_MODE = "hardcore_mode"
        private const val KEY_BREAK_COUNT = "break_count"
        private const val KEY_BREAK_ACTIVE = "break_active"
        private const val KEY_BREAK_END_TIME = "break_end_time"
        private const val MAX_BREAKS_PER_SESSION = 2

        const val DEFAULT_MESSAGE = "Şu an geleceğin için odaklanma zamanı! Masanın başına dön."
    }

    var isFirstRunCompleted: Boolean
        get() = prefs.getBoolean(KEY_FIRST_RUN, false)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_RUN, value).apply()

    var isFocusActive: Boolean
        get() = prefs.getBoolean(KEY_FOCUS_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_FOCUS_ACTIVE, value).apply()

    var focusEndTime: Long
        get() = prefs.getLong(KEY_FOCUS_END_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_FOCUS_END_TIME, value).apply()

    var focusDurationMinutes: Int
        get() = prefs.getInt(KEY_FOCUS_DURATION_MINUTES, 30)
        set(value) = prefs.edit().putInt(KEY_FOCUS_DURATION_MINUTES, value).apply()

    var customLockMessage: String
        get() = prefs.getString(KEY_CUSTOM_LOCK_MESSAGE, DEFAULT_MESSAGE) ?: DEFAULT_MESSAGE
        set(value) = prefs.edit().putString(KEY_CUSTOM_LOCK_MESSAGE, value).apply()

    var blockedApps: Set<String>
        get() = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_BLOCKED_APPS, value).apply()

    var blockedNotifications: Set<String>
        get() = prefs.getStringSet(KEY_BLOCKED_NOTIFICATIONS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_BLOCKED_NOTIFICATIONS, value).apply()

    var savedMinutes: Long
        get() = prefs.getLong(KEY_SAVED_MINUTES, 0L)
        set(value) = prefs.edit().putLong(KEY_SAVED_MINUTES, value).apply()

    var blockedAttemptsToday: Int
        get() = prefs.getInt(KEY_BLOCKED_ATTEMPTS, 0)
        set(value) = prefs.edit().putInt(KEY_BLOCKED_ATTEMPTS, value).apply()

    var isHardcoreMode: Boolean
        get() = prefs.getBoolean(KEY_HARDCORE_MODE, true)
        set(value) = prefs.edit().putBoolean(KEY_HARDCORE_MODE, value).apply()

    // Break system
    var breakCount: Int
        get() = prefs.getInt(KEY_BREAK_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_BREAK_COUNT, value).apply()

    var isBreakActive: Boolean
        get() = prefs.getBoolean(KEY_BREAK_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_BREAK_ACTIVE, value).apply()

    var breakEndTime: Long
        get() = prefs.getLong(KEY_BREAK_END_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_BREAK_END_TIME, value).apply()

    fun canTakeBreak(): Boolean = breakCount < MAX_BREAKS_PER_SESSION

    fun startBreak() {
        breakCount += 1
        isBreakActive = true
        breakEndTime = System.currentTimeMillis() + (5 * 60 * 1000L) // 5 minutes
    }

    fun checkBreakExpired(): Boolean {
        if (!isBreakActive) return false
        if (System.currentTimeMillis() >= breakEndTime) {
            isBreakActive = false
            return true // break just expired
        }
        return false
    }

    fun getBreakRemainingMillis(): Long {
        if (!isBreakActive) return 0L
        val remaining = breakEndTime - System.currentTimeMillis()
        return if (remaining > 0L) remaining else 0L
    }

    fun endFocusSession() {
        isFocusActive = false
        isBreakActive = false
        breakCount = 0
    }

    fun startFocusSession(durationMinutes: Int) {
        val durationMillis = durationMinutes * 60 * 1000L
        isFocusActive = true
        focusDurationMinutes = durationMinutes
        focusEndTime = System.currentTimeMillis() + durationMillis
        breakCount = 0
        isBreakActive = false
    }

    fun incrementBlockedAttempts() {
        blockedAttemptsToday += 1
    }

    fun isAppBlocked(packageName: String): Boolean {
        if (!isFocusActive) return false
        // During a break, don't block
        if (isBreakActive && System.currentTimeMillis() < breakEndTime) return false
        // Check break expired
        if (isBreakActive && System.currentTimeMillis() >= breakEndTime) {
            isBreakActive = false
        }
        val now = System.currentTimeMillis()
        if (now >= focusEndTime) {
            endFocusSession()
            return false
        }
        return blockedApps.contains(packageName)
    }

    fun isNotificationBlocked(packageName: String): Boolean {
        if (!isFocusActive) return false
        // During a break, allow notifications
        if (isBreakActive && System.currentTimeMillis() < breakEndTime) return false
        val now = System.currentTimeMillis()
        if (now >= focusEndTime) {
            endFocusSession()
            return false
        }
        return blockedNotifications.contains(packageName)
    }

    fun getRemainingMillis(): Long {
        if (!isFocusActive) return 0L
        val remaining = focusEndTime - System.currentTimeMillis()
        return if (remaining > 0L) remaining else 0L
    }
}
