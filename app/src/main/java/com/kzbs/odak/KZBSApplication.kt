package com.kzbs.odak

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class KZBSApplication : Application() {

    companion object {
        const val FOCUS_CHANNEL_ID = "kzbs_focus_channel"
        const val UPDATE_CHANNEL_ID = "kzbs_update_channel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusChannel = NotificationChannel(
                FOCUS_CHANNEL_ID,
                "KZBS Odak Bildirimleri",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Aktif odak sayacı ve durum bilgisi"
                setShowBadge(false)
            }

            val updateChannel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "KZBS Güncelleme Bildirimleri",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Yazılım güncellemeleri ve indirme durumu"
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(focusChannel)
            manager?.createNotificationChannel(updateChannel)
        }
    }
}
