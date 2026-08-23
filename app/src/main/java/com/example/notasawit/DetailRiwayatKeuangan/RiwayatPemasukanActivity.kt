package com.example.notasawit.DetailRiwayatKeuangan

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityRiwayatPemasukanBinding
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.notasawit.Model.ProduksiDetail
import com.example.notasawit.Model.ProduksiDetailResponse
import com.example.notasawit.Network.PetaniApi
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class RiwayatPemasukanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRiwayatPemasukanBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRiwayatPemasukanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val produksiId =
            intent.getIntExtra(
                "produksi_id",
                0
            )
        if(produksiId != 0){
            getDetailProduksi(produksiId)
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun getDetailProduksi(id:Int){
        PetaniApi.getDetailProduksi(
            id,
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException
                ){
                    runOnUiThread {
                        Toast.makeText(
                            this@RiwayatPemasukanActivity,
                            "Gagal mengambil data",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                override fun onResponse(
                    call: Call,
                    response: Response
                ) {
                    val json =
                        response.body?.string()
                    Log.d(
                        "DETAIL_CODE",
                        response.code.toString()
                    )
                    Log.d(
                        "DETAIL_BODY",
                        json ?: "NULL"
                    )

                    if (json != null && response.isSuccessful) {
                        val result =
                            Gson().fromJson(
                                json,
                                ProduksiDetailResponse::class.java
                            )

                        runOnUiThread {
                            tampilkanData(result.data)
                        }
                    } else {
                        Log.e(
                            "DETAIL_ERROR",
                            json ?: "kosong"
                        )
                    }
                }
            }
        )
    }

    private fun tampilkanData(
        data: ProduksiDetail
    ){
        binding.tvTanggal.text =
            "Tanggal : ${data.produksi_tanggal}"
        binding.tvHargaTbs.text =
            "Harga TBS : Rp ${java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(data.harga_tbs.toLong())}"
        binding.tvStatus.text =
            "Status : ${data.status_validasi}"
        binding.tvPetani.text =
            "Petani : ${data.petani?.nama ?: "-"}"

        val targetLahanId = intent.getIntExtra("lahan_id", -1).takeIf { it != -1 }
        val targetLahanNama = intent.getStringExtra("lahan_nama")
        val nominalSplit = intent.getDoubleExtra("nominal_split", -1.0).takeIf { it != -1.0 }
        val jumlahSplit = intent.getDoubleExtra("jumlah_split", -1.0).takeIf { it != -1.0 }

        val listDetail = data.detail_produksi

        val selectedDetail = if (targetLahanId != null) {
            listDetail?.find { it.lahan?.id == targetLahanId }
        } else if (!targetLahanNama.isNullOrEmpty()) {
            listDetail?.find { it.lahan?.nama.equals(targetLahanNama, ignoreCase = true) }
        } else null

        if (selectedDetail != null || !targetLahanNama.isNullOrEmpty()) {
            val namaLahan = selectedDetail?.lahan?.nama ?: targetLahanNama ?: "-"
            val jumlahTbsLahan = selectedDetail?.jumlah_tbs_detail ?: jumlahSplit ?: 0.0
            val totalPendapatanLahan = selectedDetail?.subtotal_pendapatan ?: nominalSplit ?: 0.0

            val formattedJumlah = if (jumlahTbsLahan % 1.0 == 0.0) jumlahTbsLahan.toLong().toString() else jumlahTbsLahan.toString()
            val formattedTotal = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(totalPendapatanLahan.toLong())

            binding.tvJumlahTbs.text = "Jumlah TBS : $formattedJumlah Kg"
            binding.tvTotalPendapatan.text = "Total Pendapatan : Rp $formattedTotal"
            binding.tvLahan.text = "Lahan : $namaLahan"
        } else {
            val formattedTotal = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(data.total_pendapatan.toLong())
            binding.tvJumlahTbs.text = "Jumlah TBS : ${data.jumlah_tbs} Kg"
            binding.tvTotalPendapatan.text = "Total Pendapatan : Rp $formattedTotal"

            val rincianLahanText = if (!listDetail.isNullOrEmpty()) {
                listDetail.mapIndexedNotNull { index, detail ->
                    val namaLahan = detail.lahan?.nama ?: return@mapIndexedNotNull null
                    val tbs = detail.jumlah_tbs_detail?.let {
                        if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
                    } ?: "0"
                    val subtotalVal = detail.subtotal_pendapatan
                    val subtotalStr = if (subtotalVal != null && subtotalVal > 0) {
                        val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(subtotalVal.toLong())
                        " (Rp $formatted)"
                    } else ""
                    "${index + 1}. $namaLahan: $tbs Kg$subtotalStr"
                }.joinToString("\n")
            } else {
                "-"
            }
            binding.tvLahan.text = "Rincian Lahan:\n$rincianLahanText"
        }

        binding.tvKeterangan.text =
            "Keterangan : ${data.produksi_ket ?: "-"}"

        val rawUrl = (data.produksi_bukti_url ?: data.produksi_bukti ?: "").trim()
        val fotoUrl = when {
            rawUrl.startsWith("http") -> rawUrl
            rawUrl.startsWith("storage/") -> "http://notasawit.pocari.id/$rawUrl"
            rawUrl.startsWith("produksi/") -> "http://notasawit.pocari.id/storage/$rawUrl"
            rawUrl.startsWith("/") -> "http://notasawit.pocari.id/storage$rawUrl"
            rawUrl.isNotEmpty() && rawUrl != "null" -> "http://notasawit.pocari.id/storage/produksi/$rawUrl"
            else -> ""
        }

        if (fotoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(fotoUrl)
                .placeholder(R.drawable.ic_camera)
                .error(R.drawable.ic_camera)
                .into(binding.imgBuktiProduksi)

            binding.imgBuktiProduksi.setOnClickListener {
                com.example.notasawit.Utils.CustomAlert.showImagePreview(this, fotoUrl)
            }
        } else {
            binding.imgBuktiProduksi.setImageResource(R.drawable.ic_camera)
        }
    }
}