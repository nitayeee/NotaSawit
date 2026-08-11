package com.example.notasawit.Admin.AuditInternal

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.databinding.ItemPetaniAuditBinding
import java.io.File

data class PetaniAuditData(
    val idPetani: Int,
    val namaPetani: String,
    val desa: String,
    val statusAudit: String, // "Belum Audit", "Lulus", "Perlu Perbaikan"
    val tanggalAudit: String,
    val pdfPath: String,
    val isExpanded: Boolean = false,
    val auditAttempt: Int = 0,
    val auditLabel: String = ""
)

class PetaniAuditAdapter(
    private val list: MutableList<PetaniAuditData>,
    private val onAuditClicked: (PetaniAuditData) -> Unit
) : RecyclerView.Adapter<PetaniAuditAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPetaniAuditBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PetaniAuditData, position: Int) {
            binding.tvNamaPetani.text = if (item.auditLabel.isNotEmpty()) {
                "${item.namaPetani} ${item.auditLabel}"
            } else {
                item.namaPetani
            }
            binding.tvDesa.text = item.desa
            binding.tvStatus.text = item.statusAudit

            when (item.statusAudit) {
                "Lulus" -> binding.tvStatus.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_lulus)
                "Perlu Perbaikan" -> binding.tvStatus.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_perbaikan)
                else -> binding.tvStatus.setBackgroundResource(com.example.notasawit.R.drawable.rounded_bg_gray)
            }

            if (item.statusAudit == "Belum Audit") {
                binding.tvTanggalAudit.text = "Belum ada riwayat audit di periode ini."
                binding.btnBukaPdf.visibility = View.GONE
                binding.btnAudit.text = "Lakukan Audit"
            } else {
                binding.tvTanggalAudit.text = "Tanggal Audit: ${item.tanggalAudit}"
                binding.btnBukaPdf.visibility = View.VISIBLE
                
                if (item.statusAudit == "Perlu Perbaikan") {
                    binding.btnAudit.text = "Audit Ulang"
                    binding.btnAudit.visibility = View.VISIBLE
                } else {
                    binding.btnAudit.visibility = View.GONE
                }
            }

            binding.layoutDetail.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            binding.ivExpand.rotation = if (item.isExpanded) 180f else 0f

            binding.layoutHeader.setOnClickListener {
                val expanded = !item.isExpanded
                list[position] = item.copy(isExpanded = expanded)
                notifyItemChanged(position)
            }

            binding.btnAudit.setOnClickListener {
                onAuditClicked(item)
            }

            binding.btnBukaPdf.setOnClickListener {
                openPdf(binding.root.context, item.pdfPath)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPetaniAuditBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position], position)
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<PetaniAuditData>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    private fun openPdf(context: Context, path: String) {
        if (path.isEmpty()) {
            Toast.makeText(context, "Path PDF kosong", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(path)
        if (!file.exists()) {
            Toast.makeText(context, "File PDF tidak ditemukan di perangkat ini", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            context.startActivity(Intent.createChooser(intent, "Buka PDF dengan"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal membuka PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
