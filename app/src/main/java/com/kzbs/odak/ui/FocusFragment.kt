package com.kzbs.odak.ui

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.kzbs.odak.R
import com.kzbs.odak.data.FocusPreferences
import com.kzbs.odak.databinding.FragmentFocusBinding
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
        setupStartButton()
    }

    override fun onResume() {
        super.onResume()
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

    private fun setupDurationChips() {
        val presetChips = listOf(
            binding.chip30 to 30,
            binding.chip60 to 60,
            binding.chip90 to 90
        )

        // Preset chip click listeners
        presetChips.forEach { (view, duration) ->
            view.setOnClickListener {
                if (preferences.isFocusActive) {
                    Toast.makeText(requireContext(), "Odak modu aktifken süre değiştirilemez!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                selectDuration(duration, presetChips, isCustom = false)
            }
        }

        // Custom chip click listener
        binding.chipCustom.setOnClickListener {
            if (preferences.isFocusActive) {
                Toast.makeText(requireContext(), "Odak modu aktifken süre değiştirilemez!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showCustomTimePicker(presetChips)
        }

        // Initialize selected chip
        val isPreset = presetChips.any { it.second == selectedDuration }
        selectDuration(selectedDuration, presetChips, isCustom = !isPreset)
    }

    private fun showCustomTimePicker(presetChips: List<Pair<TextView, Int>>) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(
            android.R.layout.simple_list_item_1, null // We'll build programmatically
        )

        // Build layout programmatically for custom time picker
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 16)
            setBackgroundColor(resources.getColor(R.color.bg_card))
        }

        val titleText = TextView(requireContext()).apply {
            text = getString(R.string.custom_time_title)
            textSize = 18f
            setTextColor(resources.getColor(R.color.text_primary))
            setPadding(0, 0, 0, 24)
        }
        layout.addView(titleText)

        val pickersLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER
        }

        // Hours picker
        val hoursLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(16, 0, 16, 0)
        }
        val hoursLabel = TextView(requireContext()).apply {
            text = getString(R.string.custom_time_hour)
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_muted))
            gravity = android.view.Gravity.CENTER
        }
        val hourPicker = NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = 4
            value = selectedDuration / 60
            wrapSelectorWheel = false
        }
        hoursLayout.addView(hoursLabel)
        hoursLayout.addView(hourPicker)

        // Minutes picker
        val minutesLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(16, 0, 16, 0)
        }
        val minutesLabel = TextView(requireContext()).apply {
            text = getString(R.string.custom_time_minute)
            textSize = 14f
            setTextColor(resources.getColor(R.color.text_muted))
            gravity = android.view.Gravity.CENTER
        }
        val minutePicker = NumberPicker(requireContext()).apply {
            minValue = 0
            maxValue = 55
            value = selectedDuration % 60
            // Step by 5 minutes
            displayedValues = Array(12) { "${it * 5}" }
            wrapSelectorWheel = true
        }
        minutesLayout.addView(minutesLabel)
        minutesLayout.addView(minutePicker)

        pickersLayout.addView(hoursLayout)
        pickersLayout.addView(minutesLayout)
        layout.addView(pickersLayout)

        val dialog = AlertDialog.Builder(requireContext(), R.style.Theme_KZBSODAK)
            .setView(layout)
            .setPositiveButton("Tamam") { _, _ ->
                val totalMinutes = (hourPicker.value * 60) + (minutePicker.value * 5)
                if (totalMinutes < 5) {
                    Toast.makeText(requireContext(), "Minimum süre 5 dakikadır!", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (totalMinutes > 240) {
                    Toast.makeText(requireContext(), "Maksimum süre 4 saattir!", Toast.LENGTH_SHORT).show()
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

        // Reset all preset chips
        presetChips.forEach { (view, dur) ->
            if (!isCustom && dur == duration) {
                view.setBackgroundResource(R.drawable.bg_chip_selected)
                view.setTextColor(resources.getColor(R.color.accent_emerald))
            } else {
                view.setBackgroundResource(R.drawable.bg_chip_unselected)
                view.setTextColor(resources.getColor(R.color.text_primary))
            }
        }

        // Handle custom chip appearance
        if (isCustom) {
            binding.chipCustom.setBackgroundResource(R.drawable.bg_chip_selected)
            binding.chipCustom.setTextColor(resources.getColor(R.color.accent_emerald))
            binding.chipCustom.text = "⏱️ ${duration}dk"
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

    private fun setupStartButton() {
        binding.btnStartFocus.setOnClickListener {
            if (preferences.isFocusActive) {
                Toast.makeText(requireContext(), "Tavizsiz mod aktif! Odak süresi bitmeden iptal edilemez.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (preferences.blockedApps.isEmpty() && preferences.blockedNotifications.isEmpty()) {
                Toast.makeText(requireContext(), "Lütfen 'Uygulamalar' sekmesinden en az bir uygulama seçin!", Toast.LENGTH_LONG).show()
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
    }

    private fun updateActiveStateUI() {
        if (_binding == null) return

        if (preferences.isFocusActive) {
            val remaining = preferences.getRemainingMillis()
            if (remaining <= 0) {
                preferences.isFocusActive = false
                resetToIdleUI()
                return
            }

            val hours = TimeUnit.MILLISECONDS.toHours(remaining)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(remaining) % 60
            val seconds = TimeUnit.MILLISECONDS.toSeconds(remaining) % 60
            binding.tvBigTimer.text = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

            binding.tvTimerLabel.text = "KALAN ODAK SÜRESİ"
            binding.tvTimerDurationDesc.text = "🛡️ Tavizsiz Koruma Devam Ediyor"
            binding.tvFocusStatsBadge.text = "🔥 Aktif"
            binding.tvFocusStatsBadge.setTextColor(resources.getColor(R.color.accent_crimson))

            binding.pbFocusProgress.visibility = View.VISIBLE
            val totalDuration = preferences.focusDurationMinutes * 60 * 1000L
            val progress = (((totalDuration - remaining).toDouble() / totalDuration) * 100).toInt()
            binding.pbFocusProgress.progress = progress

            binding.btnStartFocus.text = "TAVİZSİZ MOD DEVREDE (PES ETMEK YOK)"
            binding.btnStartFocus.setBackgroundResource(R.drawable.bg_button_secondary)
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
        binding.tvFocusStatsBadge.text = "🌱 Hazır"
        binding.tvFocusStatsBadge.setTextColor(resources.getColor(R.color.accent_emerald))

        binding.pbFocusProgress.visibility = View.GONE
        binding.btnStartFocus.text = getString(R.string.btn_start_focus)
        binding.btnStartFocus.setBackgroundResource(R.drawable.bg_button_emerald)
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
