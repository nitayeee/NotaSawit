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
import com.example.notasawit.databinding.ItemPetaniAuditBinding
import com.example.notasawit.databinding.ItemLahanSectionHeaderBinding
import java.io.File

data class KunjunganHistoryItem(
    val idKunjungan: String = "",
    val tanggal: String = "",
    val namaAuditor: String = "",
    val statusKunjungan: String = "",
    val pdfPath: String = "",
    val visitAttempt: Int = 1
)

data class LahanKunjunganItem(
    val lahanId: Int = 0,
    val namaLahan: String,
    val luasLahan: Double = 0.0,
    val statusLahan: String = "Belum Kunjungan",
    val latestAttempt: Int = 0,
    val history: List<KunjunganHistoryItem> = emptyList()
)

data class PetaniKunjunganData(
    val idPetani: Int,
    val namaPetani: String,
    val desa: String,
    val statusKunjungan: String,
    val tanggalKunjungan: String = "",
    val pdfPath: String = "",
    val visitAttempt: Int = 1,
    val visitLabel: String = "",
    val fotoProfil: String? = null,
    val isExpanded: Boolean = false,
    val history: List<KunjunganHistoryItem> = emptyList(),
    val lahanList: List<LahanKunjunganItem> = emptyList()
)

class PetaniKunjunganAdapter(
    private val list: MutableList<PetaniKunjunganData>,
    private val onKunjunganClicked: (PetaniKunjunganData, LahanKunjunganItem) -> Unit
) : RecyclerView.Adapter<PetaniKunjunganAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemPetaniAuditBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PetaniKunjunganData, position: Int) {
            com.example.notasawit.Utils.AvatarHelper.setupAvatar(
                binding.ivProfil,
                binding.tvInitial,
                item.namaPetani,
                item.fotoProfil
            )

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

            binding.llHistoryContainer.removeAllViews()

            if (item.lahanList.isEmpty()) {
                binding.tvBelumAda.text = "Anda belum punya lahan."
                binding.tvBelumAda.visibility = View.VISIBLE
                binding.btnAudit.visibility = View.GONE
            } else {
                binding.tvBelumAda.visibility = View.GONE
                binding.btnAudit.visibility = View.GONE

                for (lahanItem in item.lahanList) {
                    val lahanBinding = ItemLahanSectionHeaderBinding.inflate(
                        LayoutInflater.from(binding.root.context),
                        binding.llHistoryContainer,
                        false
                    )

                    val luasStr = if (lahanItem.luasLahan > 0.0) " (${lahanItem.luasLahan} Ha)" else ""
                    lahanBinding.tvNamaLahan.text = "🌾 ${lahanItem.namaLahan}$luasStr"
                    lahanBinding.tvStatusLahan.text = lahanItem.statusLahan

                    when (lahanItem.statusLahan) {
                        "Selesai", "Lulus" -> {
                            lahanBinding.tvStatusLahan.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_lulus)
                            lahanBinding.tvStatusLahan.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                        }
                        "Perlu Perbaikan" -> {
                            lahanBinding.tvStatusLahan.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_perbaikan)
                            lahanBinding.tvStatusLahan.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                        }
                        else -> {
                            lahanBinding.tvStatusLahan.setBackgroundResource(com.example.notasawit.R.drawable.rounded_bg_gray)
                            lahanBinding.tvStatusLahan.setTextColor(android.graphics.Color.parseColor("#616161"))
                        }
                    }

                    lahanBinding.llLahanHistoryContainer.removeAllViews()

                    if (lahanItem.history.isNotEmpty()) {
                        lahanBinding.tvBelumAdaLahanHistory.visibility = View.GONE
                        for (historyItem in lahanItem.history) {
                            val historyView = LayoutInflater.from(binding.root.context).inflate(
                                com.example.notasawit.R.layout.item_history_audit,
                                lahanBinding.llLahanHistoryContainer,
                                false
                            )

                            val cardHistoryItem = historyView.findViewById<com.google.android.material.card.MaterialCardView>(com.example.notasawit.R.id.cardHistoryItem)
                            val viewAccentIndicator = historyView.findViewById<View>(com.example.notasawit.R.id.viewAccentIndicator)
                            val tvAttemptTitle = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvAttemptTitle)
                            val tvAuditorName = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvAuditorName)
                            val tvStatusBadge = historyView.findViewById<android.widget.TextView>(com.example.notasawit.R.id.tvStatusBadge)
                            val btnBukaPdfHistory = historyView.findViewById<com.google.android.material.button.MaterialButton>(com.example.notasawit.R.id.btnBukaPdfHistory)

                            tvAttemptTitle.text = "Kunjungan ke-${historyItem.visitAttempt} (${historyItem.tanggal})"
                            tvAuditorName.text = "Auditor: ${historyItem.namaAuditor}"
                            tvStatusBadge.text = historyItem.statusKunjungan

                            when {
                                historyItem.statusKunjungan.equals("Selesai", ignoreCase = true) || historyItem.statusKunjungan.equals("Lulus", ignoreCase = true) -> {
                                    tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_lulus)
                                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                                    cardHistoryItem?.setCardBackgroundColor(android.graphics.Color.parseColor("#F0FDF4"))
                                    cardHistoryItem?.strokeColor = android.graphics.Color.parseColor("#C8E6C9")
                                    viewAccentIndicator?.setBackgroundColor(android.graphics.Color.parseColor("#1B5E20"))
                                    tvAttemptTitle.setTextColor(android.graphics.Color.parseColor("#1B5E20"))
                                    btnBukaPdfHistory?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1B4D2E"))
                                }
                                historyItem.statusKunjungan.equals("Perlu Perbaikan", ignoreCase = true) -> {
                                    tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_perbaikan)
                                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                                    cardHistoryItem?.setCardBackgroundColor(android.graphics.Color.parseColor("#FEF2F2"))
                                    cardHistoryItem?.strokeColor = android.graphics.Color.parseColor("#FFCCC7")
                                    viewAccentIndicator?.setBackgroundColor(android.graphics.Color.parseColor("#B71C1C"))
                                    tvAttemptTitle.setTextColor(android.graphics.Color.parseColor("#B71C1C"))
                                    btnBukaPdfHistory?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#B71C1C"))
                                }
                                historyItem.statusKunjungan.contains("Menunggu", ignoreCase = true) || historyItem.statusKunjungan.equals("Pending", ignoreCase = true) -> {
                                    tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.bg_status_diaudit)
                                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#0D47A1"))
                                    cardHistoryItem?.setCardBackgroundColor(android.graphics.Color.parseColor("#EFF6FF"))
                                    cardHistoryItem?.strokeColor = android.graphics.Color.parseColor("#BBDEFB")
                                    viewAccentIndicator?.setBackgroundColor(android.graphics.Color.parseColor("#0D47A1"))
                                    tvAttemptTitle.setTextColor(android.graphics.Color.parseColor("#0D47A1"))
                                    btnBukaPdfHistory?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#0D47A1"))
                                }
                                else -> {
                                    tvStatusBadge.setBackgroundResource(com.example.notasawit.R.drawable.rounded_bg_gray)
                                    tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#616161"))
                                    cardHistoryItem?.setCardBackgroundColor(android.graphics.Color.parseColor("#F8FAFC"))
                                    cardHistoryItem?.strokeColor = android.graphics.Color.parseColor("#E2E8F0")
                                    viewAccentIndicator?.setBackgroundColor(android.graphics.Color.parseColor("#64748B"))
                                    tvAttemptTitle.setTextColor(android.graphics.Color.parseColor("#1E293B"))
                                    btnBukaPdfHistory?.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#1B4D2E"))
                                }
                            }

                            btnBukaPdfHistory.setOnClickListener {
                                openPdf(binding.root.context, historyItem.pdfPath, historyItem.idKunjungan)
                            }

                            lahanBinding.llLahanHistoryContainer.addView(historyView)
                        }
                    } else {
                        lahanBinding.tvBelumAdaLahanHistory.visibility = View.VISIBLE
                    }

                    if (lahanItem.statusLahan == "Perlu Perbaikan") {
                        lahanBinding.btnKunjunganLahan.text = "Kunjungan Ulang"
                        lahanBinding.btnKunjunganLahan.isEnabled = true
                        lahanBinding.btnKunjunganLahan.visibility = View.VISIBLE
                    } else if (lahanItem.statusLahan == "Menunggu Keputusan") {
                        lahanBinding.btnKunjunganLahan.text = "Menunggu Hasil"
                        lahanBinding.btnKunjunganLahan.isEnabled = false
                        lahanBinding.btnKunjunganLahan.visibility = View.VISIBLE
                    } else if (lahanItem.statusLahan == "Lulus" || lahanItem.statusLahan == "Selesai") {
                        lahanBinding.btnKunjunganLahan.visibility = View.GONE
                    } else {
                        lahanBinding.btnKunjunganLahan.text = "Lakukan Kunjungan"
                        lahanBinding.btnKunjunganLahan.isEnabled = true
                        lahanBinding.btnKunjunganLahan.visibility = View.VISIBLE
                    }

                    lahanBinding.btnKunjunganLahan.setOnClickListener {
                        onKunjunganClicked(item, lahanItem)
                    }

                    binding.llHistoryContainer.addView(lahanBinding.root)
                }
            }

            binding.layoutDetail.visibility = if (item.isExpanded) View.VISIBLE else View.GONE
            binding.ivExpand.rotation = if (item.isExpanded) 180f else 0f

            binding.layoutHeader.setOnClickListener {
                val expanded = !item.isExpanded
                list[position] = item.copy(isExpanded = expanded)
                notifyItemChanged(position)
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

    private fun openPdf(context: Context, path: String, idKunjungan: String = "") {
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
            launchUrlIntent(context, cleanPath, isKunjungan = true)
            return
        }

        if (idKunjungan.isNotEmpty() && idKunjungan != "0") {
            Toast.makeText(context, "Mencari file PDF di server...", Toast.LENGTH_SHORT).show()
            fetchAndOpenPdfFromServer(context, idKunjungan, isKunjungan = true)
            return
        }

        Toast.makeText(context, "File PDF belum tersedia", Toast.LENGTH_SHORT).show()
    }

    private fun fetchAndOpenPdfFromServer(context: Context, id: String, isKunjungan: Boolean) {
        val endpoint = if (isKunjungan) "http://notasawit.pocari.id/api/kunjungan-lapangan/all" else "http://notasawit.pocari.id/api/audit-internal"
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
                            val itemAuditId = item.optString("id_kunjungan", item.optString("id", item.optString("id_audit", "")))
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
                                        "file_kunjungan", "file_pdf", "pdf_path", "path_pdf", "file_audit",
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
