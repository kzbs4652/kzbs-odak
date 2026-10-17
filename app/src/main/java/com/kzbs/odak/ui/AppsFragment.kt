package com.kzbs.odak.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
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
                filterApps(binding.etSearchApp.text?.toString().orEmpty())
            },
            onMuteToggled = { app, isMuted ->
                val current = preferences.blockedNotifications.toMutableSet()
                if (isMuted) {
                    current.add(app.packageName)
                } else {
                    current.remove(app.packageName)
                }
                preferences.blockedNotifications = current
                filterApps(binding.etSearchApp.text?.toString().orEmpty())
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
        val selectedApps = filtered.filter { it.isLocked || it.isNotificationMuted }
        val otherApps = filtered.filterNot { it.isLocked || it.isNotificationMuted }
        adapter.updateData(selectedApps, otherApps)
    }

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

            // Keep both sections in Turkish alphabetical order.
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
