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
        val rootCard: androidx.cardview.widget.CardView = view as androidx.cardview.widget.CardView
        val container: View = view.findViewById(R.id.containerNotification)
        val indicatorUnread: View = view.findViewById(R.id.indicatorUnread)
        val cvIconBackground: androidx.cardview.widget.CardView = view.findViewById(R.id.cvIconBackground)
        val ivIcon: android.widget.ImageView = view.findViewById(R.id.ivIcon)
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

        when (item.type) {
            "produksi" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_income)
                holder.ivIcon.setColorFilter(Color.parseColor("#1B4D2E"))
                holder.cvIconBackground.setCardBackgroundColor(Color.parseColor("#E8ECE9"))
            }
            "pengeluaran" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_outcome)
                holder.ivIcon.setColorFilter(Color.parseColor("#F44336"))
                holder.cvIconBackground.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
            }
            "audit" -> {
                holder.ivIcon.setImageResource(R.drawable.ic_audit)
                holder.ivIcon.setColorFilter(Color.parseColor("#FF9800"))
                holder.cvIconBackground.setCardBackgroundColor(Color.parseColor("#FFF3E0"))
            }
            else -> {
                holder.ivIcon.setImageResource(R.drawable.ic_notification)
                holder.ivIcon.setColorFilter(Color.parseColor("#9E9E9E"))
                holder.cvIconBackground.setCardBackgroundColor(Color.parseColor("#F8F9FA"))
            }
        }

        if (item.is_read == 0) {
            holder.indicatorUnread.visibility = View.VISIBLE
            holder.rootCard.setCardBackgroundColor(Color.parseColor("#E8ECE9"))
        } else {
            holder.indicatorUnread.visibility = View.INVISIBLE
            holder.rootCard.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
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
