package com.example.notasawit.Home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.databinding.ItemCarouselBinding

// Data class untuk menampung info tiap slide
// Data class baru
data class PanenItem(
    val type: String, // "WELCOME" atau "PANEN"
    val value: String, // Jumlah ton (khusus tipe panen)
    val imageRes: Int
)

class CarouselAdapter(private val items: List<PanenItem>) :
    RecyclerView.Adapter<CarouselAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCarouselBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemCarouselBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        with(holder.binding) {
            imgBanner.setImageResource(item.imageRes)

            if (item.type == "WELCOME") {
                layoutWelcome.visibility = View.VISIBLE
                layoutPanen.visibility = View.GONE
            } else {
                layoutWelcome.visibility = View.GONE
                layoutPanen.visibility = View.VISIBLE
                tvTotalPanen.text = item.value
            }
        }
    }

    override fun getItemCount(): Int = items.size
}