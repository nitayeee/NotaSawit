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
    private val onItemClick: (NotificationItem) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val container: LinearLayout = view.findViewById(R.id.containerNotification)
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvAuditor: TextView = view.findViewById(R.id.tvAuditor)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
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
            holder.container.setBackgroundColor(Color.parseColor("#E3F2FD")) 
        } else {
            holder.container.setBackgroundColor(Color.parseColor("#FFFFFF")) 
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = notifications.size
}
