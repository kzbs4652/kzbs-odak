package com.kzbs.odak.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kzbs.odak.data.AppInfo
import com.kzbs.odak.databinding.ItemAppLockBinding

class AppsAdapter(
    private val onLockToggled: (AppInfo, Boolean) -> Unit,
    private val onMuteToggled: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

    private var apps: List<AppInfo> = emptyList()

    fun updateData(apps: List<AppInfo>) {
        this.apps = apps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppLockBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position])
    }

    override fun getItemCount(): Int = apps.size

    inner class AppViewHolder(private val binding: ItemAppLockBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.tvAppName.text = app.appName
            binding.tvAppPackage.text = app.packageName
            binding.imgAppIcon.setImageDrawable(app.icon)

            binding.chipLockApp.setOnCheckedChangeListener(null)
            binding.chipMuteNotifications.setOnCheckedChangeListener(null)
            binding.chipLockApp.isChecked = app.isLocked
            binding.chipMuteNotifications.isChecked = app.isNotificationMuted

            binding.chipLockApp.setOnCheckedChangeListener { chip, isChecked ->
                app.isLocked = isChecked
                animateSelection(chip)
                onLockToggled(app, isChecked)
            }
            binding.chipMuteNotifications.setOnCheckedChangeListener { chip, isChecked ->
                app.isNotificationMuted = isChecked
                animateSelection(chip)
                onMuteToggled(app, isChecked)
            }
        }

        private fun animateSelection(view: android.view.View) {
            view.animate().cancel()
            view.scaleX = 1f
            view.scaleY = 1f
            view.animate()
                .scaleX(0.96f)
                .scaleY(0.96f)
                .setDuration(75L)
                .withEndAction {
                    view.animate().scaleX(1f).scaleY(1f).setDuration(100L).start()
                }
                .start()
        }
    }
}
