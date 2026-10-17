package com.kzbs.odak.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.kzbs.odak.R
import com.kzbs.odak.data.AppInfo
import com.kzbs.odak.data.FocusPreferences
import com.kzbs.odak.databinding.FragmentAppsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.util.Locale

class AppsFragment : Fragment() {

    private var _binding: FragmentAppsBinding? = null
    private val binding get() = _binding!!

    private lateinit var preferences: FocusPreferences
    private lateinit var adapter: AppsAdapter
    private val allApps = mutableListOf<AppInfo>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        preferences = FocusPreferences(requireContext())

        setupRecyclerView()
        setupSearch()
        loadInstalledApps()
    }

    private fun setupRecyclerView() {
        adapter = AppsAdapter(
            onLockToggled = { app, isLocked ->
                val current = preferences.blockedApps.toMutableSet()
                if (isLocked) {
                    current.add(app.packageName)
                } else {
                    current.remove(app.packageName)
                }
                preferences.blockedApps = current
                renderSelectedApps()
            },
            onMuteToggled = { app, isMuted ->
                val current = preferences.blockedNotifications.toMutableSet()
                if (isMuted) {
                    current.add(app.packageName)
                } else {
                    current.remove(app.packageName)
                }
                preferences.blockedNotifications = current
                renderSelectedApps()
            }
        )

        binding.rvAppsList.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAppsList.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchApp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterApps(query: String) {
        val trimmed = query.trim().lowercase(Locale.getDefault())
        val filtered = if (trimmed.isEmpty()) {
            allApps
        } else {
            allApps.filter {
                it.appName.lowercase(Locale.getDefault()).contains(trimmed) ||
                        it.packageName.lowercase(Locale.getDefault()).contains(trimmed)
            }
        }
        adapter.updateData(filtered)
        renderSelectedApps()
    }

    private fun renderSelectedApps() {
        val selected = allApps.filter { it.isLocked || it.isNotificationMuted }
        val panel = binding.layoutSelectedApps
        val wasVisible = panel.visibility == View.VISIBLE

        if (selected.isEmpty()) {
            if (wasVisible) {
                panel.animate().cancel()
                panel.animate()
                    .alpha(0f)
                    .translationY(-dp(6).toFloat())
                    .setDuration(140L)
                    .withEndAction {
                        panel.visibility = View.GONE
                        panel.alpha = 1f
                        panel.translationY = 0f
                    }
                    .start()
            }
            return
        }

        val chips = binding.chipSelectedApps
        chips.removeAllViews()
        selected.forEach { app ->
            val statuses = buildList {
                if (app.isLocked) add(getString(R.string.selected_app_locked))
                if (app.isNotificationMuted) add(getString(R.string.selected_app_muted))
            }
            val summary = "${app.appName} · ${statuses.joinToString(" · ")}"
            val chip = Chip(requireContext()).apply {
                text = summary
                contentDescription = "$summary, ${app.packageName}"
                isCheckable = false
                isClickable = false
                textSize = 12f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                chipBackgroundColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.bg_surface_elevated)
                )
                chipStrokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.bg_card_border)
                )
                chipStrokeWidth = dp(1).toFloat()
                chipIcon = app.icon
                isChipIconVisible = app.icon != null
                chipIconTint = null
                alpha = 0f
            }
            chips.addView(chip, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
            chip.animate().alpha(1f).setDuration(160L).start()
        }

        if (!wasVisible) {
            panel.visibility = View.VISIBLE
            panel.alpha = 0f
            panel.translationY = -dp(6).toFloat()
            panel.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180L)
                .start()
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    private fun loadInstalledApps() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val pm = requireContext().packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)

            val lockedSet = preferences.blockedApps
            val mutedSet = preferences.blockedNotifications
            val selfPackage = requireContext().packageName

            val tempList = mutableListOf<AppInfo>()

            for (resolve in resolveInfos) {
                val pkgName = resolve.activityInfo.packageName
                if (pkgName == selfPackage) continue

                val appName = resolve.loadLabel(pm).toString()
                val icon = resolve.loadIcon(pm)

                tempList.add(
                    AppInfo(
                        appName = appName,
                        packageName = pkgName,
                        icon = icon,
                        isLocked = lockedSet.contains(pkgName),
                        isNotificationMuted = mutedSet.contains(pkgName)
                    )
                )
            }

            // Keep the single app list in Turkish alphabetical order.
            val appNameCollator = Collator.getInstance(Locale("tr", "TR"))
            tempList.sortWith { first, second -> appNameCollator.compare(first.appName, second.appName) }

            withContext(Dispatchers.Main) {
                allApps.clear()
                allApps.addAll(tempList)
                filterApps("")
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
