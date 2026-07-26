package com.example.notasawit.Admin.Beranda

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.R
import java.text.SimpleDateFormat
import java.util.Locale

class PengingatAdapter(private val list: List<Pengingat>) : RecyclerView.Adapter<PengingatAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvJudul: TextView = view.findViewById(R.id.tvJudul)
        val tvPesan: TextView = view.findViewById(R.id.tvPesan)
        val tvDeadline: TextView = view.findViewById(R.id.tvDeadline)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val viewStatus: View = view.findViewById(R.id.viewStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pengingat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvJudul.text = item.judul
        holder.tvPesan.text = item.pesan
        
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val date = inputFormat.parse(item.deadline)
            holder.tvDeadline.text = if (date != null) outputFormat.format(date) else item.deadline
        } catch (e: Exception) {
            holder.tvDeadline.text = item.deadline.take(10)
        }

        if (item.isDone) {
            holder.tvStatusBadge.visibility = View.VISIBLE
            holder.tvStatusBadge.text = "Selesai"
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_lulus)
            holder.tvStatusBadge.setTextColor(Color.parseColor("#1E4620"))
            holder.viewStatus.setBackgroundColor(Color.parseColor("#9CA3AF")) // Abu-abu
        } else {
            holder.tvStatusBadge.visibility = View.VISIBLE
            holder.tvStatusBadge.text = "Pending"
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_status_perbaikan)
            holder.tvStatusBadge.setTextColor(Color.parseColor("#855B00"))
            holder.viewStatus.setBackgroundColor(Color.parseColor("#1B4332")) // Hijau
        }
    }

    override fun getItemCount(): Int = list.size
}
