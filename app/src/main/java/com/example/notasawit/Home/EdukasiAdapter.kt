package com.example.notasawit.Home

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.databinding.ItemEdukasiBinding

data class EdukasiModul(
    val id: Int,
    val title: String,
    val description: String,
    var progress: Int = 0 // 0 to 100
)

class EdukasiAdapter(
    private val listModul: List<EdukasiModul>,
    private val onClick: (EdukasiModul) -> Unit
) : RecyclerView.Adapter<EdukasiAdapter.EdukasiViewHolder>() {

    inner class EdukasiViewHolder(val binding: ItemEdukasiBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(modul: EdukasiModul) {
            binding.tvModulTitle.text = modul.title
            binding.tvModulDesc.text = modul.description
            binding.pbModulProgress.progress = modul.progress
            binding.tvProgressPercent.text = "${modul.progress}%"

            binding.root.setOnClickListener {
                onClick(modul)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EdukasiViewHolder {
        val binding = ItemEdukasiBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EdukasiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EdukasiViewHolder, position: Int) {
        holder.bind(listModul[position])
    }

    override fun getItemCount(): Int = listModul.size
}
