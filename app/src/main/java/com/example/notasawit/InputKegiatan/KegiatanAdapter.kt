package com.example.notasawit.InputKegiatan

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.R
import com.example.notasawit.Room.KegiatanPetani.KegiatanEntity

class KegiatanAdapter(
    private val context: Context,
    private var list: MutableList<KegiatanEntity>,
    private val onClick: (KegiatanEntity) -> Unit
) : RecyclerView.Adapter<KegiatanAdapter.ViewHolder>() {

    data class ItemWithLahan(
        val kegiatan: KegiatanEntity,
        val lahanNames: String
    )

    private val itemList = mutableListOf<ItemWithLahan>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvJenis: TextView = itemView.findViewById(R.id.tvJenis)
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvStatusSync: TextView = itemView.findViewById(R.id.tvStatusSync)
        val tvBahan: TextView = itemView.findViewById(R.id.tvBahan)
        val tvJumlah: TextView = itemView.findViewById(R.id.tvJumlah)
        val tvLahan: TextView = itemView.findViewById(R.id.tvLahan)
        val tvLimbah: TextView = itemView.findViewById(R.id.tvLimbah)
        val imgIconKegiatan: ImageView = itemView.findViewById(R.id.imgIconKegiatan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_kegiatan, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = itemList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemWithLahan = itemList[position]
        val item = itemWithLahan.kegiatan

        val namaKeg = item.nama_kegiatan.ifEmpty {
            when (item.kegiatan_jenis) {
                1 -> "Pemupukan"
                2 -> "Pengendalian Gulma"
                3 -> "Pengendalian Hama"
                else -> "Kegiatan Kebun"
            }
        }

        holder.tvJenis.text = namaKeg
        holder.tvTanggal.text = item.kegiatan_tanggal
        holder.tvBahan.text = item.nama_bahan.ifEmpty { item.kegiatan_ket.ifEmpty { "-" } }
        holder.tvJumlah.text = "${item.kegiatan_jumlah} ${item.kegiatan_satuan}"
        holder.tvLahan.text = itemWithLahan.lahanNames.ifEmpty { "-" }

        val limbahInfo = if (item.jenis_limbah.isNotEmpty()) {
            "${item.jenis_limbah} (${item.status_limbah})"
        } else {
            "Tidak ada limbah"
        }
        holder.tvLimbah.text = limbahInfo

        if (item.isSynced) {
            holder.tvStatusSync.text = "Tersinkron"
            holder.tvStatusSync.setBackgroundResource(R.drawable.bg_badge_green)
            holder.tvStatusSync.setTextColor(android.graphics.Color.parseColor("#1B4D2E"))
        } else {
            holder.tvStatusSync.text = "Mengantre Sync"
            holder.tvStatusSync.setBackgroundResource(R.drawable.bg_badge_red)
            holder.tvStatusSync.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    fun updateItems(data: List<ItemWithLahan>) {
        itemList.clear()
        itemList.addAll(data)
        notifyDataSetChanged()
    }

    fun setData(data: List<KegiatanEntity>) {
        list.clear()
        list.addAll(data)
        itemList.clear()
        itemList.addAll(data.map { ItemWithLahan(it, "-") })
        notifyDataSetChanged()
    }
}