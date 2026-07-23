package com.example.notasawit.Notifikasi

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.R

class NotificationAdapter(
    private val notifications: List<NotificationItem>,
    private val onItemClick: (NotificationItem) -> Unit,
    private val onItemLongClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    var isSelectionMode = false

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.containerNotification)
        val indicatorUnread: View = view.findViewById(R.id.indicatorUnread)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvAuditor: TextView = view.findViewById(R.id.tvAuditor)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val cbSelect: android.widget.CheckBox = view.findViewById(R.id.cbSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = notifications[position]
        
        holder.tvTitle.text = item.title
        holder.tvAuditor.text = item.message
        holder.tvTanggal.text = "Tanggal: ${item.tanggal}"

        if (item.is_read == 0) {
            holder.container.setBackgroundColor(Color.parseColor("#E8EEE8")) 
            holder.indicatorUnread.visibility = View.VISIBLE
        } else {
            holder.container.setBackgroundColor(Color.parseColor("#FFFFFF")) 
            holder.indicatorUnread.visibility = View.INVISIBLE
        }

        if (isSelectionMode) {
            holder.cbSelect.visibility = View.VISIBLE
            holder.cbSelect.isChecked = item.isSelected
        } else {
            holder.cbSelect.visibility = View.GONE
            holder.cbSelect.isChecked = false
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true // true indicates the long click event was consumed
        }
    }

    override fun getItemCount() = notifications.size
}
