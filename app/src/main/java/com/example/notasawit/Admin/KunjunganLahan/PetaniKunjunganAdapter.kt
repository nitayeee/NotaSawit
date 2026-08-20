package com.example.notasawit.Admin.KunjunganLahan

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.example.notasawit.Room.KunjunganLahanEntity.KunjunganLahanForm
import com.example.notasawit.databinding.ItemPetaniAuditBinding
import java.io.File

data class PetaniKunjunganData(
    val idPetani: Int,
    val namaPetani: String,
    val desa: String,
    val statusKunjungan: String, // "Belum Kunjungan", "Selesai", "Perlu Perbaikan"
    val tanggalKunjungan: String,
    val pdfPath: String,
    val isExpanded: Boolean = false,
    val visitAttempt: Int = 0,
    val visitLabel: String = "",
    val history: List<KunjunganLahanForm> = emptyList()
)

class PetaniKunjunganAdapter(
    private val list: MutableList<PetaniKunjunganData>,
    private val onKunjunganClicked: (PetaniKunjunganData) -> Unit
) : RecyclerView.Adapter<PetaniKunjunganAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPetaniAuditBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PetaniKunjunganData, position: Int) {
            binding.tvNamaPetani.text = if (item.visitLabel.isNotEmpty()) {
                "${item.namaPetani} ${item.visitLabel}"
            } else {
                item.namaPetani
            }
            binding.tvDesa.text = item.desa
            binding.tvStatus.text = item.statusKunjungan

            when (item.statusKunjungan) {
                "Selesai", "Lulus" -> binding.tvStatus.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_lulus)
                "Perlu Perbaikan" -> binding.tvStatus.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_perbaikan)
                else -> binding.tvStatus.setBackgroundResource(com.example.notasawit.R.drawable.rounded_bg_gray)
            }

            binding.tvBelumAda.visibility = if (item.statusKunjungan == "Belum Kunjungan" || item.statusKunjungan == "Belum Audit") View.VISIBLE else View.GONE
            if (binding.tvBelumAda.visibility == View.VISIBLE) {
                binding.tvBelumAda.text = "Belum ada riwayat kunjungan lahan di periode ini."
            }
            
            binding.llHistoryContainer.removeAllViews()

            if (item.statusKunjungan != "Belum Kunjungan" && item.statusKunjungan != "Belum Audit" && item.history.isNotEmpty()) {
                for (historyItem in item.history) {
                    val historyView = LayoutInflater.from(binding.root.context).inflate(com.example.notasawit.R.layout.item_history_audit, binding.llHistoryContainer, false)
                    
                    val tvAttemptTitle = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvAttemptTitle)
                    val tvAuditorName = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvAuditorName)
                    val tvStatusBadge = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvStatusBadge)
                    val btnBukaPdfHistory = historyView.findViewById<com.google.android.material.button.MaterialButton>(com.example.notasawit.R.id.btnBukaPdfHistory)

                    tvAttemptTitle.text = "Kunjungan ke-${historyItem.visitAttempt} (${historyItem.tanggal})"
                    tvAuditorName.text = "Auditor: ${historyItem.namaAuditor}"
                    tvStatusBadge.text = historyItem.statusKunjungan

                    when (historyItem.statusKunjungan) {
                        "Selesai", "Lulus" -> tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_lulus)
                        "Perlu Perbaikan" -> tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_perbaikan)
                        else -> tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.rounded_bg_gray)
                    }

                    btnBukaPdfHistory.setOnClickListener {
                        openPdf(binding.root.context, historyItem.pdfPath)
                    }

                    binding.llHistoryContainer.addView(historyView)
                }

                if (item.statusKunjungan == "Perlu Perbaikan") {
                    binding.btnAudit.text = "Kunjungan Ulang"
                    binding.btnAudit.isEnabled = true
                    binding.btnAudit.visibility = View.VISIBLE
                } else if (item.statusKunjungan == "Menunggu Keputusan") {
                    binding.btnAudit.text = "Menunggu Hasil"
                    binding.btnAudit.isEnabled = false
                    binding.btnAudit.visibility = View.VISIBLE
                } else {
                    binding.btnAudit.visibility = View.GONE
                }
            } else {
                binding.btnAudit.text = "Lakukan Kunjungan"
                binding.btnAudit.isEnabled = true
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
                onKunjunganClicked(item)
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

    fun updateData(newList: List<PetaniKunjunganData>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }

    private fun openPdf(context: Context, path: String) {
        if (path.isEmpty()) {
            Toast.makeText(context, "Path PDF kosong", Toast.LENGTH_SHORT).show()
            return
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(path))
            context.startActivity(intent)
            return
        }

        val file = File(path)
        if (file.exists()) {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                context.startActivity(Intent.createChooser(intent, "Buka PDF dengan"))
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val url = if (!path.startsWith("storage/")) "http://160.187.144.157/storage/$path" else "http://160.187.144.157/$path"
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "File PDF tidak dapat dibuka", Toast.LENGTH_SHORT).show()
        }
    }
}
