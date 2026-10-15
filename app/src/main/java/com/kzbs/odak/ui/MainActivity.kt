package com.kzbs.odak.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kzbs.odak.R
import com.kzbs.odak.databinding.ActivityMainBinding
import com.kzbs.odak.update.UpdateChecker

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val focusFragment = FocusFragment()
    private val appsFragment = AppsFragment()
    private val settingsFragment = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()

        // Check for updates quietly in background on startup
        UpdateChecker(this).checkForUpdates(showUpToDateToast = false)
    }

    private fun setupNavigation() {
        // Default to focus tab
        loadFragment(focusFragment)

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_focus -> {
                    loadFragment(focusFragment)
                    true
                }
                R.id.navigation_apps -> {
                    loadFragment(appsFragment)
                    true
                }
                R.id.navigation_settings -> {
                    loadFragment(settingsFragment)
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
