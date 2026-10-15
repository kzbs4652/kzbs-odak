package com.kzbs.odak.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.kzbs.odak.R
import com.kzbs.odak.data.FocusPreferences
import com.kzbs.odak.databinding.ActivityLockOverlayBinding
import java.util.Locale
import java.util.concurrent.TimeUnit

class OverlayLockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    private lateinit var binding: ActivityLockOverlayBinding
    private lateinit var preferences: FocusPreferences
    private val handler = Handler(Looper.getMainLooper())

    private val countdownRunnable = object : Runnable {
        override fun run() {
            updateLockUI()
            if (preferences.isFocusActive) {
                handler.postDelayed(this, 1000L)
            } else {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make this activity appear on top of everything — critical for Samsung One UI
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        binding = ActivityLockOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = FocusPreferences(this)

        // Block hardware Back button — go HOME, do NOT return to blocked app
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                goToHomeScreen()
            }
        })

        setupBlockedAppInfo()
        setupUserMotivationMessage()
        setupReturnHomeButton()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        // Update blocked app info when re-launched for a different app
        setupBlockedAppInfo()
        setupUserMotivationMessage()
    }

    override fun onResume() {
        super.onResume()
        // Double-check focus is still active
        if (!preferences.isFocusActive) {
            finish()
            return
        }
        handler.post(countdownRunnable)
    }

    override fun onPause() {
        handler.removeCallbacks(countdownRunnable)
        super.onPause()
    }

    // Block recent apps button on Samsung One UI
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus && preferences.isFocusActive) {
            // User might be trying to switch apps via recents
            // Bring ourselves back to front after a short delay
            handler.postDelayed({
                if (preferences.isFocusActive && !isFinishing) {
                    val bringBackIntent = Intent(this, OverlayLockActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }
                    startActivity(bringBackIntent)
                }
            }, 300)
        }
    }

    // Block volume and other hardware keys from doing anything unexpected
    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event != null) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK,
                KeyEvent.KEYCODE_APP_SWITCH -> {
                    goToHomeScreen()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun setupBlockedAppInfo() {
        val targetPkg = intent.getStringExtra(EXTRA_PACKAGE_NAME)
        if (!targetPkg.isNullOrEmpty()) {
            try {
                val appInfo = packageManager.getApplicationInfo(targetPkg, 0)
                val appName = packageManager.getApplicationLabel(appInfo).toString()
                binding.tvBlockedAppBadge.text = "🔒 $appName Erişimi Kısıtlandı"
            } catch (e: PackageManager.NameNotFoundException) {
                binding.tvBlockedAppBadge.text = "🔒 Bu Uygulamaya Giriş Kısıtlandı"
            }
        }
    }

    private fun setupUserMotivationMessage() {
        binding.tvUserLockMessage.text = "\"${preferences.customLockMessage}\""
    }

    private fun setupReturnHomeButton() {
        binding.btnReturnHome.setOnClickListener {
            goToHomeScreen()
        }
    }

    private fun goToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun updateLockUI() {
        val remaining = preferences.getRemainingMillis()
        if (remaining <= 0) {
            finish()
            return
        }

        val hours = TimeUnit.MILLISECONDS.toHours(remaining)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
        binding.tvLockCountdown.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

        val totalDuration = preferences.focusDurationMinutes * 60 * 1000L
        val progress = (((totalDuration - remaining).toDouble() / totalDuration) * 100).toInt()
        binding.pbLockSession.progress = progress
        binding.tvLockProgressPercent.text = "%$progress Tamamlandı"
    }
}
