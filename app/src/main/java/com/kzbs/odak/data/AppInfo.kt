package com.kzbs.odak.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val appName: String,
    val packageName: String,
    val icon: Drawable?,
    var isLocked: Boolean = false,
    var isNotificationMuted: Boolean = false
)
