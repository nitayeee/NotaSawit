package com.example.notasawit.Admin.Tugas

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.Admin.Beranda.Pengingat
import com.example.notasawit.R
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class TugasAdapter(
    private var list: List<Pengingat>,
    private val onSelesaiClick: (Pengingat) -> Unit
) : RecyclerView.Adapter<TugasAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvJudulTugas: TextView = view.findViewById(R.id.tvJudulTugas)
        val tvPesanTugas: TextView = view.findViewById(R.id.tvPesanTugas)
        val tvDeadlineTugas: TextView = view.findViewById(R.id.tvDeadlineTugas)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        val btnSelesaiTugas: MaterialButton = view.findViewById(R.id.btnSelesaiTugas)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tugas, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvJudulTugas.text = item.judul
        holder.tvPesanTugas.text = item.pesan

        val deadlineStr = item.deadline
        if (!deadlineStr.isNullOrEmpty()) {
            try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
                val date = inputFormat.parse(deadlineStr.take(10))
                holder.tvDeadlineTugas.text = "Deadline: " + (if (date != null) outputFormat.format(date) else deadlineStr)
            } catch (e: Exception) {
                holder.tvDeadlineTugas.text = "Deadline: ${deadlineStr.take(10)}"
            }
        } else {
            holder.tvDeadlineTugas.text = "Deadline: -"
        }

        if (item.isDone) {
            holder.tvStatusBadge.text = "SELESAI"
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_rounded_gray)
            holder.tvStatusBadge.setTextColor(Color.parseColor("#475569"))
            holder.btnSelesaiTugas.visibility = View.GONE
        } else {
            holder.tvStatusBadge.text = "PENDING"
            holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_rounded_gold)
            holder.tvStatusBadge.setTextColor(Color.parseColor("#744210"))
            holder.btnSelesaiTugas.visibility = View.VISIBLE
            holder.btnSelesaiTugas.setOnClickListener {
                onSelesaiClick(item)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Pengingat>) {
        this.list = newList
        notifyDataSetChanged()
    }
}
