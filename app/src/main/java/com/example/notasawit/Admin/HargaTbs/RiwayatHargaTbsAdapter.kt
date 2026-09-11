package com.example.notasawit.Admin.HargaTbs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.Model.HargaTbsItem
import com.example.notasawit.databinding.ItemRiwayatHargaTbsBinding
import java.text.NumberFormat
import java.util.Locale

class RiwayatHargaTbsAdapter(
    private val list: List<HargaTbsItem>
) : RecyclerView.Adapter<RiwayatHargaTbsAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRiwayatHargaTbsBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRiwayatHargaTbsBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val fmtDinas = NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.harga_dinas.toLong())
        val fmtPtSar = NumberFormat.getNumberInstance(Locale("id", "ID")).format(item.harga_pt_sar.toLong())

        holder.binding.tvTanggalBerlaku.text = "Berlaku: ${item.tanggal_berlaku}"
        holder.binding.tvTanggalDiubah.text = "Diubah: ${item.created_at ?: item.tanggal_berlaku}"
        holder.binding.tvHargaDinas.text = "Rp $fmtDinas / Kg"
        holder.binding.tvHargaPtSar.text = "Rp $fmtPtSar / Kg"

        if (position == 0) {
            holder.binding.tvBadgeOrder.text = "Terbaru"
            holder.binding.tvBadgeOrder.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_green)
        } else {
            holder.binding.tvBadgeOrder.text = "#${position + 1}"
            holder.binding.tvBadgeOrder.setBackgroundResource(com.example.notasawit.R.drawable.bg_rounded_gray)
        }
    }

    override fun getItemCount(): Int = list.size
}
