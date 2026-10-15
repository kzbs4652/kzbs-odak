package com.kzbs.odak.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.kzbs.odak.data.AppInfo
import com.kzbs.odak.databinding.ItemAppLockBinding

class AppsAdapter(
    private var appsList: List<AppInfo>,
    private val onLockToggled: (AppInfo, Boolean) -> Unit,
    private val onMuteToggled: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<AppsAdapter.AppViewHolder>() {

    fun updateData(newApps: List<AppInfo>) {
        appsList = newApps
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppLockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(appsList[position])
    }

    override fun getItemCount(): Int = appsList.size

    inner class AppViewHolder(private val binding: ItemAppLockBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.tvAppName.text = app.appName
            binding.tvAppPackage.text = app.packageName
            if (app.icon != null) {
                binding.imgAppIcon.setImageDrawable(app.icon)
            }

            // Remove listeners before setting checked state to avoid trigger loop
            binding.switchLockApp.setOnCheckedChangeListener(null)
            binding.switchMuteNotification.setOnCheckedChangeListener(null)

            binding.switchLockApp.isChecked = app.isLocked
            binding.switchMuteNotification.isChecked = app.isNotificationMuted

            binding.switchLockApp.setOnCheckedChangeListener { _, isChecked ->
                app.isLocked = isChecked
                onLockToggled(app, isChecked)
            }

            binding.switchMuteNotification.setOnCheckedChangeListener { _, isChecked ->
                app.isNotificationMuted = isChecked
                onMuteToggled(app, isChecked)
            }
        }
    }
}
