package com.example.notasawit.Home.Panen

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.R
import com.example.notasawit.databinding.ItemPanenBulananBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class PanenBulanItem(
    val monthIndex: Int, // 1 - 12
    val year: Int,
    val monthName: String,
    val monthAbbr: String,
    val yearAbbr: String,
    val totalKg: Double,
    val totalRp: Double,
    val transactionCount: Int,
    val percentageFromPrev: Double? = null
)

class PanenBulananAdapter(
    private val onItemClick: ((PanenBulanItem) -> Unit)? = null
) : RecyclerView.Adapter<PanenBulananAdapter.PanenViewHolder>() {

    private val items = mutableListOf<PanenBulanItem>()
    private val df = DecimalFormat("#,##0.#", DecimalFormatSymbols(Locale("id", "ID")))
    private val currencyFormat = java.text.NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    fun submitList(newItems: List<PanenBulanItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PanenViewHolder {
        val binding = ItemPanenBulananBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PanenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PanenViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PanenViewHolder(
        private val binding: ItemPanenBulananBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PanenBulanItem) {
            binding.tvItemBulanAbbr.text = item.monthAbbr
            binding.tvItemTahunAbbr.text = item.yearAbbr
            binding.tvItemBulanNama.text = "${item.monthName} ${item.year}"

            if (item.transactionCount > 0) {
                binding.tvItemTransaksi.text = "${item.transactionCount} Transaksi Panen"
            } else {
                binding.tvItemTransaksi.text = "Tidak ada transaksi"
            }

            val rpFormatted = currencyFormat.format(item.totalRp).replace("Rp", "Rp ")
            binding.tvItemPendapatan.text = rpFormatted

            // Total Kg
            if (item.totalKg >= 1000.0) {
                val tonVal = item.totalKg / 1000.0
                binding.tvItemTotalPanen.text = "${df.format(tonVal)} Ton"
                binding.tvItemTonUnit.text = "${df.format(item.totalKg)} Kg"
            } else {
                binding.tvItemTotalPanen.text = "${df.format(item.totalKg)} Kg"
                val tonVal = item.totalKg / 1000.0
                binding.tvItemTonUnit.text = "${String.format(Locale("id", "ID"), "%.2f", tonVal)} Ton"
            }

            // Tren badge
            val pct = item.percentageFromPrev
            if (pct != null && item.totalKg > 0) {
                if (pct > 0) {
                    binding.tvItemBadgeTren.visibility = android.view.View.VISIBLE
                    binding.tvItemBadgeTren.text = "↑ +${String.format(Locale("id", "ID"), "%.0f", pct)}%"
                    binding.tvItemBadgeTren.setBackgroundResource(R.drawable.bg_badge_green)
                    binding.tvItemBadgeTren.setTextColor(Color.parseColor("#1B4D2E"))
                } else if (pct < 0) {
                    binding.tvItemBadgeTren.visibility = android.view.View.VISIBLE
                    binding.tvItemBadgeTren.text = "↓ ${String.format(Locale("id", "ID"), "%.0f", pct)}%"
                    binding.tvItemBadgeTren.setBackgroundResource(R.drawable.bg_badge_red)
                    binding.tvItemBadgeTren.setTextColor(Color.parseColor("#C62828"))
                } else {
                    binding.tvItemBadgeTren.visibility = android.view.View.VISIBLE
                    binding.tvItemBadgeTren.text = "Stabil"
                    binding.tvItemBadgeTren.setBackgroundResource(R.drawable.bg_badge_green)
                    binding.tvItemBadgeTren.setTextColor(Color.parseColor("#1B4D2E"))
                }
            } else if (item.totalKg > 0) {
                binding.tvItemBadgeTren.visibility = android.view.View.VISIBLE
                binding.tvItemBadgeTren.text = "Aktif"
                binding.tvItemBadgeTren.setBackgroundResource(R.drawable.bg_badge_green)
                binding.tvItemBadgeTren.setTextColor(Color.parseColor("#1B4D2E"))
            } else {
                binding.tvItemBadgeTren.visibility = android.view.View.GONE
            }

            binding.root.setOnClickListener {
                onItemClick?.invoke(item)
            }
        }
    }
}

