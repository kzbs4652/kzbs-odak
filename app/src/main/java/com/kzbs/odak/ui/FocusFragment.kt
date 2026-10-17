package com.kzbs.odak.ui

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.kzbs.odak.R
import com.kzbs.odak.data.FocusPreferences
import com.kzbs.odak.databinding.FragmentFocusBinding
import com.kzbs.odak.service.FocusAccessibilityService
import com.kzbs.odak.service.FocusForegroundService
import java.util.Locale
import java.util.concurrent.TimeUnit

class FocusFragment : Fragment() {

    private var _binding: FragmentFocusBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferences: FocusPreferences
    private val handler = Handler(Looper.getMainLooper())
    private var selectedDuration = 30 // minutes — default

    private val tickerRunnable = object : Runnable {
        override fun run() {
            if (_binding == null) return
            updateActiveStateUI()
            if (preferences.isFocusActive) {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = FocusPreferences(requireContext())
        selectedDuration = preferences.focusDurationMinutes

        setupDurationChips()
        setupMessageEditor()
        setupButtons()
    }

    override fun onResume() {
        super.onResume()
        checkPermissionsState()
        updateProtectionStats()
        updateActiveStateUI()
        if (preferences.isFocusActive) {
            handler.post(tickerRunnable)
        }
    }

    override fun onPause() {
        handler.removeCallbacks(tickerRunnable)
        super.onPause()
    }

    private fun checkPermissionsState() {
        if (_binding == null) return
        val hasAccessibility = isAccessibilityServiceEnabled(requireContext())
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(requireContext())
        } else true

        if (!hasAccessibility || !hasOverlay) {
            binding.layoutPermissionWarning.visibility = View.VISIBLE
            binding.tvPermissionWarningDesc.text = if (!hasAccessibility) {
                "Uygulamaların açılmasını anında engellemek için 'KZBS Odak Kilitleyici' servisini AÇIK yapmalısınız."
            } else {
                "Kilit ekranının Instagram üzerine çıkabilmesi için 'Üstte Gösterme' izni verilmelidir."
            }
            binding.btnFixPermissions.setOnClickListener {
                if (!hasAccessibility) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                    Toast.makeText(requireContext(), "\"Yüklü Uygulamalar\" > \"KZBS Odak Kilitleyici\" servisini AÇIK yapın!", Toast.LENGTH_LONG).show()
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${requireContext().packageName}")
                        )
                        startActivity(intent)
                    }
                }
            }
        } else {
            binding.layoutPermissionWarning.visibility = View.GONE
        }
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

    private fun setupDurationChips() {
        val presetChips = listOf(
            binding.chip30 to 30,
            binding.chip60 to 60,
            binding.chip90 to 90
        )

        presetChips.forEach { (view, duration) ->
            view.setOnClickListener {
                if (preferences.isFocusActive) {
                    Toast.makeText(requireContext(), "Odak modu aktifken süre değiştirilemez!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                selectDuration(duration, presetChips, isCustom = false)
            }
        }

        binding.chipCustom.setOnClickListener {
            if (preferences.isFocusActive) {
                Toast.makeText(requireContext(), "Odak modu aktifken süre değiştirilemez!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showCustomTimePicker(presetChips)
        }

        val isPreset = presetChips.any { it.second == selectedDuration }
        selectDuration(selectedDuration, presetChips, isCustom = !isPreset)
    }

    private fun showCustomTimePicker(presetChips: List<Pair<TextView, Int>>) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 36, 48, 16)
        }

        val titleText = TextView(requireContext()).apply {
            text = "Özel Süre Seçimi"
            textSize = 18f
            setTextColor(resources.getColor(R.color.text_primary))
            setPadding(0, 0, 0, 24)
        }
        layout.addView(titleText)

        val pickersLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        // Hours picker (0 - 8 hours)
        val hoursLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(16, 0, 24, 0)
        }
        val hoursLabel = TextView(requireContext()).apply {
            text = "Saat"
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_muted))
            gravity = android.view.Gravity.CENTER
        }
        val hourPicker = NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = 8
            value = (selectedDuration / 60).coerceIn(0, 8)
            wrapSelectorWheel = false
        }
        hoursLayout.addView(hoursLabel)
        hoursLayout.addView(hourPicker)

        // Minutes picker (0 to 59 minutes, 1-minute increments)
        val minutesLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(24, 0, 16, 0)
        }
        val minutesLabel = TextView(requireContext()).apply {
            text = "Dakika"
            textSize = 13f
            setTextColor(resources.getColor(R.color.text_muted))
            gravity = android.view.Gravity.CENTER
        }
        val minutePicker = NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = 59
            displayedValues = Array(60) { "$it dk" }
            value = (selectedDuration % 60).coerceIn(0, 59)
            wrapSelectorWheel = true
        }
        minutesLayout.addView(minutesLabel)
        minutesLayout.addView(minutePicker)

        pickersLayout.addView(hoursLayout)
        pickersLayout.addView(minutesLayout)
        layout.addView(pickersLayout)

        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_KZBSODAK)
            .setView(layout)
            .setPositiveButton("Ayarla") { _, _ ->
                val totalMinutes = (hourPicker.value * 60) + minutePicker.value
                if (totalMinutes < 1) {
                    Toast.makeText(requireContext(), "Minimum süre 1 dakikadır!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                selectDuration(totalMinutes, presetChips, isCustom = true)
            }
            .setNegativeButton("İptal", null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_card_dark)
        dialog.show()
    }

    private fun selectDuration(duration: Int, presetChips: List<Pair<TextView, Int>>, isCustom: Boolean) {
        selectedDuration = duration
        preferences.focusDurationMinutes = duration

        presetChips.forEach { (view, dur) ->
            if (!isCustom && dur == duration) {
                view.setBackgroundResource(R.drawable.bg_chip_selected)
                view.setTextColor(resources.getColor(R.color.accent_emerald))
            } else {
                view.setBackgroundResource(R.drawable.bg_chip_unselected)
                view.setTextColor(resources.getColor(R.color.text_primary))
            }
        }

        if (isCustom) {
            binding.chipCustom.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.chipCustom.setTextColor(resources.getColor(R.color.accent_emerald))
            val h = duration / 60
            val m = duration % 60
            binding.chipCustom.text = if (h > 0) "⏱️ ${h}s ${m}d" else "⏱️ ${m}dk"
        } else {
            binding.chipCustom.setBackgroundResource(R.drawable.bg_chip_unselected)
            binding.chipCustom.setTextColor(resources.getColor(R.color.text_primary))
            binding.chipCustom.text = getString(R.string.custom_time)
        }

        val hours = duration / 60
        val mins = duration % 60
        binding.tvBigTimer.text = String.format(Locale.getDefault(), "%02d:%02d:00", hours, mins)
        binding.tvTimerDurationDesc.text = "$duration Dakika Derin Odak"
    }

    private fun setupMessageEditor() {
        binding.etFocusLockMessage.setText(preferences.customLockMessage)
        binding.etFocusLockMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val newMsg = s?.toString()?.trim()
                if (!newMsg.isNullOrEmpty()) {
                    preferences.customLockMessage = newMsg
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupButtons() {
        // Start Focus button
        binding.btnStartFocus.setOnClickListener {
            // Check permissions first
            if (!isAccessibilityServiceEnabled(requireContext())) {
                showPermissionRequiredDialog()
                return@setOnClickListener
            }

            if (preferences.blockedApps.isEmpty() && preferences.blockedNotifications.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen 'Uygulamalar' sekmesinden kilitlenecek en az bir uygulama seçin!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val durationMillis = selectedDuration * 60 * 1000L
            preferences.isFocusActive = true
            preferences.focusEndTime = System.currentTimeMillis() + durationMillis

            FocusForegroundService.startService(requireContext())
            Toast.makeText(requireContext(), "KZBS ODAK Başlatıldı! Masanın başına dön.", Toast.LENGTH_SHORT).show()

            updateActiveStateUI()
            handler.post(tickerRunnable)
        }

        // End Focus button (Visible only when focus is active)
        binding.btnEndFocus.setOnClickListener {
            showEndSessionConfirmDialog()
        }
    }

    private fun showPermissionRequiredDialog() {
        AlertDialog.Builder(requireContext(), R.style.Theme_KZBSODAK)
            .setTitle("⚠️ Kilit İzni Gerekli")
            .setMessage("Instagram ve diğer seçtiğiniz uygulamaların giriş anında engellenmesi için 'KZBS Odak Kilitleyici' erişilebilirlik servisini AÇIK yapmalısınız.\n\nŞimdi ayarları açmak ister misiniz?")
            .setPositiveButton("Ayarları Aç") { _, _ ->
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
                Toast.makeText(requireContext(), "\"Yüklü Uygulamalar\" > \"KZBS Odak Kilitleyici\" servisini AÇIK yapın!", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Daha Sonra", null)
            .show()
    }

    private fun showEndSessionConfirmDialog() {
        val remaining = preferences.getRemainingMillis()
        val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining)

        AlertDialog.Builder(requireContext(), R.style.Theme_KZBSODAK)
            .setTitle(getString(R.string.end_session_title))
            .setMessage("Odak seansını sonlandırmak istediğine emin misin?\nKalan süre: $minutes dakika.")
            .setPositiveButton("Evet, Bitir") { _, _ ->
                preferences.endFocusSession()
                FocusForegroundService.stopService(requireContext())
                Toast.makeText(requireContext(), "Odak seansı sonlandırıldı.", Toast.LENGTH_SHORT).show()
                updateActiveStateUI()
            }
            .setNegativeButton("Vazgeçme, Masaya Dön", null)
            .show()
    }

    private fun updateActiveStateUI() {
        if (_binding == null) return

        if (preferences.isFocusActive) {
            val remaining = preferences.getRemainingMillis()
            if (remaining <= 0) {
                preferences.endFocusSession()
                FocusForegroundService.stopService(requireContext())
                resetToIdleUI()
                return
            }

            val hours = TimeUnit.MILLISECONDS.toHours(remaining)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
            binding.tvBigTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

            binding.tvTimerLabel.text = "KALAN ODAK SÜRESİ"
            binding.tvTimerDurationDesc.visibility = View.GONE
            binding.tvFocusStatsBadge.text = "🔥 Aktif"
            binding.tvFocusStatsBadge.setTextColor(resources.getColor(R.color.accent_crimson))

            binding.pbFocusProgress.visibility = View.VISIBLE
            val totalDuration = preferences.focusDurationMinutes * 60 * 1000L
            val progress = (((totalDuration - remaining).toDouble() / totalDuration) * 100).toInt()
            binding.pbFocusProgress.progress = progress

            binding.btnStartFocus.visibility = View.GONE
            binding.btnEndFocus.visibility = View.VISIBLE
            binding.layoutDurationChips.alpha = 0.5f
            binding.etFocusLockMessage.isEnabled = false
        } else {
            resetToIdleUI()
        }
    }

    private fun resetToIdleUI() {
        binding.tvTimerLabel.text = "ODAK SÜRESİ"
        val hours = selectedDuration / 60
        val mins = selectedDuration % 60
        binding.tvBigTimer.text = String.format(Locale.getDefault(), "%02d:%02d:00", hours, mins)
        binding.tvTimerDurationDesc.text = "$selectedDuration Dakika Derin Odak"
        binding.tvTimerDurationDesc.visibility = View.VISIBLE
        binding.tvFocusStatsBadge.text = "🌱 Hazır"
        binding.tvFocusStatsBadge.setTextColor(resources.getColor(R.color.accent_emerald))

        binding.pbFocusProgress.visibility = View.GONE
        binding.btnStartFocus.visibility = View.VISIBLE
        binding.btnEndFocus.visibility = View.GONE
        binding.layoutDurationChips.alpha = 1.0f
        binding.etFocusLockMessage.isEnabled = true
    }

    private fun updateProtectionStats() {
        if (_binding == null) return
        binding.tvCountBlockedApps.text = preferences.blockedApps.size.toString()
        binding.tvCountBlockedNotifications.text = preferences.blockedNotifications.size.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
