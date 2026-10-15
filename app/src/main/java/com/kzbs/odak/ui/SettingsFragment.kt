package com.kzbs.odak.ui

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.fragment.app.Fragment
import com.kzbs.odak.BuildConfig
import com.kzbs.odak.R
import com.kzbs.odak.data.FocusPreferences
import com.kzbs.odak.databinding.FragmentSettingsBinding
import com.kzbs.odak.service.FocusAccessibilityService
import com.kzbs.odak.update.UpdateChecker

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferences: FocusPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = FocusPreferences(requireContext())

        setupVersionAndUpdates()
        setupPermissionClickListeners()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsHealth()
    }

    private fun setupVersionAndUpdates() {
        binding.tvCurrentVersion.text = "Mevcut Sürüm: v${BuildConfig.VERSION_NAME}"

        binding.btnCheckUpdates.setOnClickListener {
            binding.tvUpdateStatus.text = getString(R.string.checking_updates)

            UpdateChecker(requireActivity()).checkForUpdates(
                showUpToDateToast = false,
                onComplete = { _ -> },
                onStatusMessage = { message ->
                    if (_binding != null) {
                        binding.tvUpdateStatus.text = message
                    }
                }
            )
        }
    }

    private fun setupPermissionClickListeners() {
        val context = requireContext()

        // Perm 0: Accessibility
        binding.rowPermAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(context, "\"Yüklü Uygulamalar\" > \"KZBS Odak Kilitleyici\" servisini AÇIK yapın!", Toast.LENGTH_LONG).show()
        }

        // Perm 1: Usage Stats
        binding.rowPermUsage.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        // Perm 2: Overlay
        binding.statusPermOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                startActivity(intent)
            }
        }

        // Perm 3: Notification Access
        binding.statusPermNotification.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        // Perm 4: Battery Optimization
        binding.statusPermBattery.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                startActivity(intent)
            }
        }
    }

    private fun checkPermissionsHealth() {
        val context = requireContext()

        // 0. Accessibility Service
        val hasAccessibility = isAccessibilityServiceEnabled(context)
        binding.statusPermAccessibility.text = if (hasAccessibility) "TAMAM ✓" else "EKSİK (AÇIN) !"
        binding.statusPermAccessibility.setTextColor(
            resources.getColor(if (hasAccessibility) R.color.accent_emerald else R.color.accent_crimson)
        )

        // 1. Usage stats
        val hasUsage = hasUsageAccess(context)
        binding.statusPermUsage.text = if (hasUsage) "TAMAM ✓" else "EKSİK !"
        binding.statusPermUsage.setTextColor(
            resources.getColor(if (hasUsage) R.color.accent_emerald else R.color.accent_crimson)
        )

        // 2. Overlay
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
        binding.statusPermOverlay.text = if (hasOverlay) "TAMAM ✓" else "EKSİK !"
        binding.statusPermOverlay.setTextColor(
            resources.getColor(if (hasOverlay) R.color.accent_emerald else R.color.accent_crimson)
        )

        // 3. Notification Access
        val packages = NotificationManagerCompat.getEnabledListenerPackages(context)
        val hasNotif = packages.contains(context.packageName)
        binding.statusPermNotification.text = if (hasNotif) "TAMAM ✓" else "EKSİK !"
        binding.statusPermNotification.setTextColor(
            resources.getColor(if (hasNotif) R.color.accent_emerald else R.color.accent_crimson)
        )

        // 4. Battery Optimization
        val hasBattery = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            pm.isIgnoringBatteryOptimizations(context.packageName)
        } else true
        binding.statusPermBattery.text = if (hasBattery) "TAMAM ✓" else "EKSİK !"
        binding.statusPermBattery.setTextColor(
            resources.getColor(if (hasBattery) R.color.accent_emerald else R.color.accent_crimson)
        )
    }

    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expectedComponentName = ComponentName(context, FocusAccessibilityService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledComponent = ComponentName.unflattenFromString(componentNameString)
            if (enabledComponent != null && enabledComponent == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
