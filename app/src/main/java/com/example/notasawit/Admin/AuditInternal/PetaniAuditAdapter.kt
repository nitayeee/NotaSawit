package com.example.notasawit.Admin.AuditInternal

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
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
    val fotoProfil: String? = null,
    val history: List<AuditHeader> = emptyList()
)

class PetaniAuditAdapter(
    private val list: MutableList<PetaniAuditData>,
    private val onAuditClicked: (PetaniAuditData) -> Unit
) : RecyclerView.Adapter<PetaniAuditAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPetaniAuditBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PetaniAuditData, position: Int) {
            com.example.notasawit.Utils.AvatarHelper.setupAvatar(
                binding.ivProfil,
                binding.tvInitial,
                item.namaPetani,
                item.fotoProfil
            )

            binding.tvNamaPetani.text = if (item.auditLabel.isNotEmpty()) {
                "${item.namaPetani} ${item.auditLabel}"
            } else {
                item.namaPetani
            }
            binding.tvDesa.text = item.desa
            binding.tvStatus.text = item.statusAudit

            when {
                item.statusAudit.equals("Lulus", ignoreCase = true) -> {
                    binding.tvStatus.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_lulus)
                    binding.tvStatus.setTextColor(Color.parseColor("#1B5E20"))
                }
                item.statusAudit.equals("Perlu Perbaikan", ignoreCase = true) -> {
                    binding.tvStatus.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_perbaikan)
                    binding.tvStatus.setTextColor(Color.parseColor("#B71C1C"))
                }
                item.statusAudit.contains("Menunggu", ignoreCase = true) || item.statusAudit.equals("Pending", ignoreCase = true) -> {
                    binding.tvStatus.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_diaudit)
                    binding.tvStatus.setTextColor(Color.parseColor("#0D47A1"))
                }
                else -> {
                    binding.tvStatus.setBackgroundResource(com.example.notasawit.R.drawable.rounded_bg_gray)
                    binding.tvStatus.setTextColor(Color.parseColor("#616161"))
                }
            }

            binding.tvBelumAda.visibility = if (item.statusAudit == "Belum Audit") View.VISIBLE else View.GONE
            binding.llHistoryContainer.removeAllViews()

            if (item.statusAudit != "Belum Audit" && item.history.isNotEmpty()) {
                for (historyItem in item.history) {
                    val historyView = LayoutInflater.from(binding.root.context).inflate(com.example.notasawit.R.layout.item_history_audit, binding.llHistoryContainer, false)
                    
                    val cardHistoryItem = historyView.findViewById<com.google.android.material.card.MaterialCardView>(com.example.notasawit.R.id.cardHistoryItem)
                    val viewAccentIndicator = historyView.findViewById<View>(com.example.notasawit.R.id.viewAccentIndicator)
                    val tvAttemptTitle = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvAttemptTitle)
                    val tvAuditorName = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvAuditorName)
                    val tvStatusBadge = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvStatusBadge)
                    val btnBukaPdfHistory = historyView.findViewById<com.google.android.material.button.MaterialButton>(com.example.notasawit.R.id.btnBukaPdfHistory)

                    tvAttemptTitle.text = "Audit ke-${historyItem.auditAttempt} (${historyItem.tanggal})"
                    tvAuditorName.text = "Auditor: ${historyItem.namaAuditor}"
                    tvStatusBadge.text = historyItem.statusAudit

                    when {
                        historyItem.statusAudit.equals("Lulus", ignoreCase = true) -> {
                            tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_lulus)
                            tvStatusBadge.setTextColor(Color.parseColor("#1B5E20"))
                            cardHistoryItem?.setCardBackgroundColor(Color.parseColor("#F0FDF4"))
                            cardHistoryItem?.strokeColor = Color.parseColor("#C8E6C9")
                            viewAccentIndicator?.setBackgroundColor(Color.parseColor("#1B5E20"))
                            tvAttemptTitle.setTextColor(Color.parseColor("#1B5E20"))
                            btnBukaPdfHistory?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1B4D2E"))
                        }
                        historyItem.statusAudit.equals("Perlu Perbaikan", ignoreCase = true) -> {
                            tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_perbaikan)
                            tvStatusBadge.setTextColor(Color.parseColor("#B71C1C"))
                            cardHistoryItem?.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
                            cardHistoryItem?.strokeColor = Color.parseColor("#FFCCC7")
                            viewAccentIndicator?.setBackgroundColor(Color.parseColor("#B71C1C"))
                            tvAttemptTitle.setTextColor(Color.parseColor("#B71C1C"))
                            btnBukaPdfHistory?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#B71C1C"))
                        }
                        historyItem.statusAudit.contains("Menunggu", ignoreCase = true) || historyItem.statusAudit.equals("Pending", ignoreCase = true) -> {
                            tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_diaudit)
                            tvStatusBadge.setTextColor(Color.parseColor("#0D47A1"))
                            cardHistoryItem?.setCardBackgroundColor(Color.parseColor("#EFF6FF"))
                            cardHistoryItem?.strokeColor = Color.parseColor("#BBDEFB")
                            viewAccentIndicator?.setBackgroundColor(Color.parseColor("#0D47A1"))
                            tvAttemptTitle.setTextColor(Color.parseColor("#0D47A1"))
                            btnBukaPdfHistory?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#0D47A1"))
                        }
                        else -> {
                            tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.rounded_bg_gray)
                            tvStatusBadge.setTextColor(Color.parseColor("#616161"))
                            cardHistoryItem?.setCardBackgroundColor(Color.parseColor("#F8FAFC"))
                            cardHistoryItem?.strokeColor = Color.parseColor("#E2E8F0")
                            viewAccentIndicator?.setBackgroundColor(Color.parseColor("#64748B"))
                            tvAttemptTitle.setTextColor(Color.parseColor("#1E293B"))
                            btnBukaPdfHistory?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1B4D2E"))
                        }
                    }

                    btnBukaPdfHistory.setOnClickListener {
                        openPdf(binding.root.context, historyItem.pdfPath, historyItem.idAudit)
                    }

                    binding.llHistoryContainer.addView(historyView)
                }

                if (item.statusAudit == "Perlu Perbaikan") {
                    binding.btnAudit.text = "Audit Ulang"
                    binding.btnAudit.isEnabled = true
                    binding.btnAudit.visibility = View.VISIBLE
                } else if (item.statusAudit == "Menunggu Keputusan") {
                    binding.btnAudit.text = "Menunggu Hasil"
                    binding.btnAudit.isEnabled = false
                    binding.btnAudit.visibility = View.VISIBLE
                } else {
                    binding.btnAudit.text = "+ Audit Lahan Lain"
                    binding.btnAudit.isEnabled = true
                    binding.btnAudit.visibility = View.VISIBLE
                }
            } else {
                binding.btnAudit.text = "Lakukan Audit"
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

    private fun openPdf(context: Context, path: String, idAudit: String = "") {
        var cleanPath = path.trim()

        if (cleanPath.startsWith("/storage/") || cleanPath.startsWith("/data/user")) {
            val localFile = File(cleanPath)
            if (localFile.exists() && localFile.length() > 0) {
                try {
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", localFile)
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(Intent.createChooser(intent, "Buka PDF dengan"))
                    return
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                cleanPath = localFile.name
            }
        }

        if (cleanPath.isNotEmpty() && cleanPath != "null") {
            launchUrlIntent(context, cleanPath, isKunjungan = false)
            return
        }

        if (idAudit.isNotEmpty() && idAudit != "0") {
            Toast.makeText(context, "Mencari file PDF di server...", Toast.LENGTH_SHORT).show()
            fetchAndOpenPdfFromServer(context, idAudit, isKunjungan = false)
            return
        }

        Toast.makeText(context, "File PDF belum tersedia", Toast.LENGTH_SHORT).show()
    }

    private fun fetchAndOpenPdfFromServer(context: Context, id: String, isKunjungan: Boolean) {
        val endpoint = if (isKunjungan) "http://notasawit.pocari.id/api/kunjungan-lahan" else "http://notasawit.pocari.id/api/audit-internal"
        val request = okhttp3.Request.Builder().url(endpoint).build()
        val client = okhttp3.OkHttpClient()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                (context as? android.app.Activity)?.runOnUiThread {
                    Toast.makeText(context, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    try {
                        val dataArray: org.json.JSONArray = when {
                            body.trim().startsWith("[") -> org.json.JSONArray(body)
                            else -> {
                                val jsonObject = org.json.JSONObject(body)
                                jsonObject.optJSONArray("data")
                                    ?: jsonObject.optJSONArray("audits")
                                    ?: jsonObject.optJSONArray("kunjungan")
                                    ?: org.json.JSONArray()
                            }
                        }

                        var foundPath = ""

                        for (i in 0 until dataArray.length()) {
                            val item = dataArray.getJSONObject(i)
                            val itemAuditId = item.optString("id_audit", item.optString("id", item.optString("id_kunjungan", "")))
                            if (itemAuditId.equals(id, ignoreCase = true)) {
                                val keys = item.keys()
                                while (keys.hasNext()) {
                                    val k = keys.next()
                                    val v = item.optString(k, "").trim()
                                    if (v.contains(".pdf", ignoreCase = true) && v != "null") {
                                        foundPath = v
                                        break
                                    }
                                }

                                if (foundPath.isEmpty()) {
                                    val possibleKeys = listOf(
                                        "file_pdf", "file_audit", "pdf_path", "path_pdf", "file_kunjungan",
                                        "file", "pdf", "url_pdf", "file_url", "pdf_file", "link_pdf", "url", "path"
                                    )
                                    for (key in possibleKeys) {
                                        if (item.has(key) && !item.isNull(key)) {
                                            val valStr = item.optString(key, "").trim()
                                            if (valStr.isNotEmpty() && valStr != "null") {
                                                foundPath = valStr
                                                break
                                            }
                                        }
                                    }
                                }
                                break
                            }
                        }

                        (context as? android.app.Activity)?.runOnUiThread {
                            if (foundPath.isNotEmpty()) {
                                launchUrlIntent(context, foundPath, isKunjungan)
                            } else {
                                Toast.makeText(context, "File PDF belum diunggah di server", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        (context as? android.app.Activity)?.runOnUiThread {
                            Toast.makeText(context, "Gagal memproses data PDF dari server", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    (context as? android.app.Activity)?.runOnUiThread {
                        Toast.makeText(context, "File PDF belum tersedia di server", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun launchUrlIntent(context: Context, path: String, isKunjungan: Boolean = false) {
        var clean = path.trim()
        if (clean.startsWith("/") || clean.contains("/emulated/") || clean.contains("/Android/data/")) {
            clean = File(clean).name
        }

        val fullUrl = when {
            clean.startsWith("http://") || clean.startsWith("https://") -> clean
            else -> {
                var relative = clean
                    .replace("public/", "")
                    .replace("storage/app/public/", "")
                    .replace("storage/", "")
                    .replace("^/+".toRegex(), "")

                if (!relative.contains("/")) {
                    relative = if (isKunjungan) "kunjungan_lapangan/$relative" else "audit_internal/$relative"
                }

                "http://notasawit.pocari.id/storage/$relative"
            }
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka link PDF", Toast.LENGTH_SHORT).show()
        }
    }
}
