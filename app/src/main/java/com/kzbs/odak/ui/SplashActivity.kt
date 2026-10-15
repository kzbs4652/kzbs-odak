package com.kzbs.odak.ui

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kzbs.odak.data.FocusPreferences
import com.kzbs.odak.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var preferences: FocusPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferences = FocusPreferences(this)

        startSplashAnimation()
    }

    private fun startSplashAnimation() {
        // Logo gentle scale breathing animation
        val scaleX = ObjectAnimator.ofFloat(binding.imgLogo, "scaleX", 0.9f, 1.05f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(binding.imgLogo, "scaleY", 0.9f, 1.05f, 1.0f)
        scaleX.duration = 1400
        scaleY.duration = 1400
        scaleX.interpolator = AccelerateDecelerateInterpolator()
        scaleY.interpolator = AccelerateDecelerateInterpolator()
        scaleX.start()
        scaleY.start()

        // Progress bar smooth advance
        lifecycleScope.launch {
            binding.tvSplashStatus.text = "Sistem hazırlanıyor..."
            binding.progressBarSplash.progress = 25
            delay(400)

            binding.tvSplashStatus.text = "Uygulama listesi taranıyor..."
            binding.progressBarSplash.progress = 65
            delay(450)

            binding.tvSplashStatus.text = "KZBS Odak Alanı Hazır!"
            binding.progressBarSplash.progress = 100
            delay(350)

            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        if (!preferences.isFirstRunCompleted) {
            val intent = Intent(this, OnboardingActivity::class.java)
            startActivity(intent)
        } else {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
