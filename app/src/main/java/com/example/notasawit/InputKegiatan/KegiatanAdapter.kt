package com.example.notasawit.InputKegiatan

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.R
import com.example.notasawit.Room.KegiatanPetani.KegiatanEntity

class KegiatanAdapter(
    private val context: Context,
    private var list: MutableList<KegiatanEntity>,
    private val onClick: (KegiatanEntity) -> Unit
) : RecyclerView.Adapter<KegiatanAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvJenis: TextView = itemView.findViewById(R.id.tvJenis)
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvLahan: TextView = itemView.findViewById(R.id.tvLahan)
        val tvJumlah: TextView = itemView.findViewById(R.id.tvJumlah)

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_riwayat_kegiatan, parent, false)

        return ViewHolder(view)

    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = list[position]

        holder.tvTanggal.text = item.kegiatan_tanggal

        holder.tvJumlah.text =
            "${item.kegiatan_jumlah} ${item.kegiatan_satuan}"

        // sementara
//        holder.tvJenis.text = item.kegiatan_jenis

        holder.tvLahan.text = "-"

        holder.itemView.setOnClickListener {

            onClick(item)

        }

    }

    fun setData(data: List<KegiatanEntity>) {

        list.clear()
        list.addAll(data)
        notifyDataSetChanged()

    }

}