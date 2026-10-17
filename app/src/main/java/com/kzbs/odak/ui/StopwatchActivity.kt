package com.kzbs.odak.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.kzbs.odak.R
import com.kzbs.odak.databinding.ActivityStopwatchBinding
import java.util.Locale

class StopwatchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStopwatchBinding
    private val handler = Handler(Looper.getMainLooper())

    private var countdownNumber = 3
    private var stopwatchStartedAt = 0L
    private var elapsedBeforePause = 0L
    private var isRunning = false
    private var immersiveMode = false
    private var tapCount = 0
    private var lastTapAt = 0L

    private val countdownRunnable = object : Runnable {
        override fun run() {
            if (countdownNumber > 1) {
                countdownNumber -= 1
                binding.tvStopwatchCountdown.text = countdownNumber.toString()
                handler.postDelayed(this, COUNTDOWN_INTERVAL_MS)
            } else {
                binding.tvStopwatchCountdown.visibility = View.GONE
                binding.tvStopwatchTime.visibility = View.VISIBLE
                startStopwatch()
            }
        }
    }

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            binding.tvStopwatchTime.text = formatDuration(currentElapsedMillis())
            handler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStopwatchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartStopwatch.setOnClickListener { beginCountdown() }
        binding.layoutStopwatchScreen.setOnClickListener {
            if (isRunning) registerStopTap()
        }
        binding.btnStopwatchReturnHome.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        if (immersiveMode) hideSystemBars(window)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && immersiveMode) hideSystemBars(window)
    }

    override fun onBackPressed() {
        if (isRunning) {
            Toast.makeText(this, R.string.stopwatch_triple_tap_hint, Toast.LENGTH_SHORT).show()
        } else {
            super.onBackPressed()
        }
    }

    private fun beginCountdown() {
        binding.layoutStopwatchSetup.visibility = View.GONE
        binding.layoutStopwatchSummary.visibility = View.GONE
        binding.layoutStopwatchScreen.visibility = View.VISIBLE
        binding.layoutStopwatchScreen.setBackgroundColor(android.graphics.Color.BLACK)
        binding.tvStopwatchCountdown.visibility = View.VISIBLE
        binding.tvStopwatchCountdown.text = COUNTDOWN_START.toString()
        binding.tvStopwatchTime.visibility = View.GONE

        countdownNumber = COUNTDOWN_START
        elapsedBeforePause = 0L
        immersiveMode = true
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemBars(window)
        handler.postDelayed(countdownRunnable, COUNTDOWN_INTERVAL_MS)
    }

    private fun startStopwatch() {
        stopwatchStartedAt = SystemClock.elapsedRealtime()
        isRunning = true
        binding.tvStopwatchTime.text = formatDuration(elapsedBeforePause)
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
    }

    private fun registerStopTap() {
        val now = SystemClock.elapsedRealtime()
        if (lastTapAt == 0L || now - lastTapAt > TAP_GAP_LIMIT_MS) {
            tapCount = 1
        } else {
            tapCount += 1
        }
        lastTapAt = now

        if (tapCount >= REQUIRED_TAPS) {
            tapCount = 0
            lastTapAt = 0L
            pauseStopwatch()
        }
    }

    private fun pauseStopwatch() {
        elapsedBeforePause = currentElapsedMillis()
        isRunning = false
        handler.removeCallbacks(tickRunnable)
        binding.tvStopwatchTime.text = formatDuration(elapsedBeforePause)

        val dialog = AlertDialog.Builder(this, R.style.Theme_KZBSODAK)
            .setTitle(R.string.stopwatch_paused_title)
            .setMessage(getString(R.string.stopwatch_cancel_message, formatDuration(elapsedBeforePause)))
            .setPositiveButton(R.string.stopwatch_cancel) { _, _ -> showSummary() }
            .setNegativeButton(R.string.stopwatch_continue) { _, _ -> resumeStopwatch() }
            .setCancelable(false)
            .create()

        dialog.setOnDismissListener { if (immersiveMode) hideSystemBars(window) }
        dialog.show()
        dialog.window?.let(::hideSystemBars)
    }

    private fun resumeStopwatch() {
        stopwatchStartedAt = SystemClock.elapsedRealtime()
        isRunning = true
        tapCount = 0
        lastTapAt = 0L
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
    }

    private fun showSummary() {
        isRunning = false
        handler.removeCallbacks(tickRunnable)
        binding.layoutStopwatchScreen.visibility = View.GONE
        binding.layoutStopwatchSummary.visibility = View.VISIBLE
        binding.tvStopwatchSummaryTime.text =
            getString(R.string.stopwatch_summary_elapsed, formatDuration(elapsedBeforePause))
    }

    private fun currentElapsedMillis(): Long =
        if (isRunning) elapsedBeforePause + SystemClock.elapsedRealtime() - stopwatchStartedAt
        else elapsedBeforePause

    private fun formatDuration(durationMillis: Long): String {
        val totalSeconds = durationMillis / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds / 60L) % 60L
        val seconds = totalSeconds % 60L
        return String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    private fun hideSystemBars(targetWindow: Window) {
        WindowCompat.setDecorFitsSystemWindows(targetWindow, false)
        WindowInsetsControllerCompat(targetWindow, targetWindow.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onDestroy()
    }

    private companion object {
        const val COUNTDOWN_START = 3
        const val COUNTDOWN_INTERVAL_MS = 1000L
        const val TICK_INTERVAL_MS = 200L
        const val REQUIRED_TAPS = 3
        const val TAP_GAP_LIMIT_MS = 900L
    }
}
