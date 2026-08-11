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
import com.example.notasawit.Room.AuditEntity.AuditHeader
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
    val auditLabel: String = "",
    val history: List<AuditHeader> = emptyList()
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

            binding.tvBelumAda.visibility = if (item.statusAudit == "Belum Audit") View.VISIBLE else View.GONE
            binding.llHistoryContainer.removeAllViews()

            if (item.statusAudit != "Belum Audit" && item.history.isNotEmpty()) {
                for (historyItem in item.history) {
                    val historyView = LayoutInflater.from(binding.root.context).inflate(com.example.notasawit.R.layout.item_history_audit, binding.llHistoryContainer, false)
                    
                    val tvAttemptTitle = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvAttemptTitle)
                    val tvAuditorName = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvAuditorName)
                    val tvStatusBadge = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvStatusBadge)
                    val btnBukaPdfHistory = historyView.findViewById<com.google.android.material.button.MaterialButton>(com.example.notasawit.R.id.btnBukaPdfHistory)

                    tvAttemptTitle.text = "Audit ke-${historyItem.auditAttempt} (${historyItem.tanggal})"
                    tvAuditorName.text = "Auditor: ${historyItem.namaAuditor}"
                    tvStatusBadge.text = historyItem.statusAudit

                    when (historyItem.statusAudit) {
                        "Lulus" -> tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_lulus)
                        "Perlu Perbaikan" -> tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_perbaikan)
                        else -> tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.rounded_bg_gray)
                    }

                    btnBukaPdfHistory.setOnClickListener {
                        openPdf(binding.root.context, historyItem.pdfPath)
                    }

                    binding.llHistoryContainer.addView(historyView)
                }

                if (item.statusAudit == "Perlu Perbaikan") {
                    binding.btnAudit.text = "Audit Ulang"
                    binding.btnAudit.visibility = View.VISIBLE
                } else {
                    binding.btnAudit.visibility = View.GONE
                }
            } else {
                binding.btnAudit.text = "Lakukan Audit"
                binding.btnAudit.visibility = View.VISIBLE
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
