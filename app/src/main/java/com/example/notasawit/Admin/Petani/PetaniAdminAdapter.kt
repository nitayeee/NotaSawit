package com.example.notasawit.Admin.Petani

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.Utils.AvatarHelper
import com.example.notasawit.databinding.ItemPetaniAdminBinding

data class PetaniAdminItem(
    val idPetani: Int,
    val namaPetani: String,
    val username: String,
    val desaId: Int,
    val namaDesa: String,
    val email: String? = null,
    val noHp: String? = null,
    val alamat: String? = null,
    val status: String = "Aktif",
    val fotoProfil: String? = null
)

class PetaniAdminAdapter(
    private val onItemClick: (PetaniAdminItem) -> Unit
) : RecyclerView.Adapter<PetaniAdminAdapter.PetaniViewHolder>() {

    private val items = mutableListOf<PetaniAdminItem>()

    fun submitList(newItems: List<PetaniAdminItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetaniViewHolder {
        val binding = ItemPetaniAdminBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PetaniViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PetaniViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class PetaniViewHolder(
        private val binding: ItemPetaniAdminBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PetaniAdminItem) {
            binding.tvNamaPetani.text = item.namaPetani
            binding.tvUsername.text = if (item.username.isNotEmpty()) "@${item.username}" else "-"
            binding.tvDesaBadge.text = if (item.namaDesa.isNotEmpty()) "Desa ${item.namaDesa}" else "Desa ID: ${item.desaId}"
            binding.tvStatusBadge.text = item.status

            AvatarHelper.setupAvatar(
                imageView = binding.ivProfil,
                textViewInitial = binding.tvInitial,
                nama = item.namaPetani,
                fotoPathOrUrl = item.fotoProfil
            )

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }
}

