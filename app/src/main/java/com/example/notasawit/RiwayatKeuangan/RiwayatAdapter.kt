package com.example.notasawit.RiwayatKeuangan

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.R
import com.example.notasawit.databinding.ItemTransaksiBinding
import java.text.NumberFormat
import java.util.Locale

class RiwayatAdapter(
    private var list: MutableList<RiwayatItem>
) : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    inner class ViewHolder(
        val binding: ItemTransaksiBinding
    ) : RecyclerView.ViewHolder(binding.root)

    var onItemClick: ((RiwayatItem) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val binding = ItemTransaksiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val item = list[position]
        val id= item.id

        holder.binding.tvTitle.text = item.judul
        holder.binding.tvDate.text = item.tanggal

        if (item.isRead == 0) {
            holder.binding.root.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
        } else {
            holder.binding.root.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
        }

        when (item.tipe.lowercase()) {

            "pemasukan" -> {

                holder.binding.tvAmount.text =
                    "+ Rp ${formatRupiah(item.nominal)}"

                holder.binding.tvAmount.setTextColor(
                    Color.parseColor("#2E7D32")
                )

                holder.binding.ivIcon.setImageResource(
                    R.drawable.ic_profile
                )
            }

            else -> {

                holder.binding.tvAmount.text =
                    "- Rp ${formatRupiah(item.nominal)}"

                holder.binding.tvAmount.setTextColor(
                    Color.parseColor("#D32F2F")
                )

                holder.binding.ivIcon.setImageResource(
                    R.drawable.ic_riwayat
                )
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
        holder.binding.ivChevron.setOnClickListener {
            onItemClick?.invoke(item)
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun updateData(
        newList: List<RiwayatItem>
    ) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    private fun formatRupiah(
        value: Double
    ): String {

        val localeID = Locale("in", "ID")

        return NumberFormat
            .getNumberInstance(localeID)
            .format(value)
    }
}