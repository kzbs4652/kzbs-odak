package com.kzbs.odak.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationManagerCompat
import com.kzbs.odak.R
import com.kzbs.odak.data.FocusPreferences
import com.kzbs.odak.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private lateinit var preferences: FocusPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = FocusPreferences(this)

        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionButtonsState()
    }

    private fun setupButtons() {
        // 1. Accessibility Service (MOST CRITICAL)
        binding.btnPermAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "\"KZBS Odak Kilitleyici\" servisini bulup AÇIK yapın!", Toast.LENGTH_LONG).show()
        }

        // 2. Usage Access
        binding.btnPermUsage.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        // 3. Overlay
        binding.btnPermOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        // 4. Notification Listener
        binding.btnPermNotification.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        // 5. Battery Optimization
        binding.btnPermBattery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }

        // Complete Onboarding
        binding.btnCompleteOnboarding.setOnClickListener {
            val customMessage = binding.etInitialLockMessage.text.toString().trim()
            if (customMessage.isNotEmpty()) {
                preferences.customLockMessage = customMessage
            }

            preferences.isFirstRunCompleted = true
            Toast.makeText(this, "KZBS ODAK Kurulumu Tamamlandı!", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun updatePermissionButtonsState() {
        // 1. Accessibility Service
        if (isAccessibilityServiceEnabled()) {
            binding.btnPermAccessibility.text = getString(R.string.perm_granted)
            binding.btnPermAccessibility.setBackgroundResource(R.drawable.bg_chip_selected)
        }

        // 2. Usage stats
        if (hasUsageAccess()) {
            binding.btnPermUsage.text = getString(R.string.perm_granted)
            binding.btnPermUsage.setBackgroundResource(R.drawable.bg_chip_selected)
        }

        // 3. Overlay
        if (hasOverlayPermission()) {
            binding.btnPermOverlay.text = getString(R.string.perm_granted)
            binding.btnPermOverlay.setBackgroundResource(R.drawable.bg_chip_selected)
        }

        // 4. Notification Access
        if (hasNotificationAccess()) {
            binding.btnPermNotification.text = getString(R.string.perm_granted)
            binding.btnPermNotification.setBackgroundResource(R.drawable.bg_chip_selected)
        }

        // 5. Battery Optimization
        if (isBatteryOptimizationIgnored()) {
            binding.btnPermBattery.text = getString(R.string.perm_granted)
            binding.btnPermBattery.setBackgroundResource(R.drawable.bg_chip_selected)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        for (info in enabledServices) {
            val id = info.resolveInfo?.serviceInfo
            if (id != null && id.packageName == packageName &&
                id.name == "com.kzbs.odak.service.FocusAccessibilityService"
            ) {
                return true
            }
        }
        return false
    }

    private fun hasUsageAccess(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val packages = NotificationManagerCompat.getEnabledListenerPackages(this)
        return packages.contains(packageName)
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }
}
