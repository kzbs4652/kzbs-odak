package com.kzbs.odak.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.kzbs.odak.R
import com.kzbs.odak.data.AppInfo
import com.kzbs.odak.databinding.ItemAppLockBinding

class AppsAdapter(
    private val onLockToggled: (AppInfo, Boolean) -> Unit,
    private val onMuteToggled: (AppInfo, Boolean) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private data class Row(val sectionTitle: String? = null, val app: AppInfo? = null)
    private var rows: List<Row> = emptyList()

    fun updateData(selectedApps: List<AppInfo>, otherApps: List<AppInfo>) {
        rows = buildList {
            if (selectedApps.isNotEmpty()) {
                add(Row(sectionTitle = "Seçilen Uygulamalar"))
                selectedApps.forEach { add(Row(app = it)) }
            }
            add(Row(sectionTitle = "Uygulamalar"))
            otherApps.forEach { add(Row(app = it)) }
        }
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (rows[position].sectionTitle != null) VIEW_TYPE_SECTION else VIEW_TYPE_APP

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SECTION) {
            SectionViewHolder(inflater.inflate(R.layout.item_apps_section, parent, false))
        } else {
            AppViewHolder(ItemAppLockBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val row = rows[position]
        when (holder) {
            is SectionViewHolder -> holder.bind(row.sectionTitle.orEmpty())
            is AppViewHolder -> row.app?.let(holder::bind)
        }
    }

    override fun getItemCount(): Int = rows.size

    private class SectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(title: String) {
            (itemView as TextView).text = title
        }
    }

    inner class AppViewHolder(private val binding: ItemAppLockBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.tvAppName.text = app.appName
            binding.tvAppPackage.text = app.packageName
            app.icon?.let(binding.imgAppIcon::setImageDrawable)

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

    private companion object {
        const val VIEW_TYPE_SECTION = 0
        const val VIEW_TYPE_APP = 1
    }
}
