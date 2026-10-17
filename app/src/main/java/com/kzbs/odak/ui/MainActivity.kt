package com.kzbs.odak.ui

import android.os.Bundle
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
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
        binding.toolbarMain.setNavigationOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        loadFragment(focusFragment)
        binding.navigationView.setCheckedItem(R.id.navigation_focus)

        binding.navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_focus -> {
                    loadFragment(focusFragment)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.navigation_apps -> {
                    loadFragment(appsFragment)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.navigation_settings -> {
                    loadFragment(settingsFragment)
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.navigation_stopwatch -> {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    startActivity(Intent(this, StopwatchActivity::class.java))
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

    override fun onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
