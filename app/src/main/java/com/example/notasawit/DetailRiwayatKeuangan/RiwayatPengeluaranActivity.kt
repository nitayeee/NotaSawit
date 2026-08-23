package com.example.notasawit.DetailRiwayatKeuangan

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.notasawit.Model.BiayaDetailResponse
import com.example.notasawit.Model.PengeluaranDetail
import com.example.notasawit.Model.ProduksiDetail
import com.example.notasawit.Model.ProduksiDetailResponse
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityRiwayatPemasukanBinding
import com.example.notasawit.databinding.ActivityRiwayatPengeluaranBinding
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class RiwayatPengeluaranActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRiwayatPengeluaranBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRiwayatPengeluaranBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val biayaId =
            intent.getIntExtra(
                "biaya_id",
                0
            )
        if(biayaId != 0){
            getDetailBiayaOperasional(biayaId)
        }
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun getDetailBiayaOperasional(id:Int){
        PetaniApi.getDetailBiayaOperasional(
            id,
            object : Callback {
                override fun onFailure(
                    call: Call,
                    e: IOException
                ){
                    runOnUiThread {
                        Toast.makeText(
                            this@RiwayatPengeluaranActivity,
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
                                BiayaDetailResponse::class.java
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
        data: PengeluaranDetail
    ){
        Log.d("BUKTI", data.biaya_bukti ?: "null")

        binding.tvTanggal.text =
            "Tanggal : ${data.biaya_tanggal}"
        binding.tvNamaBiaya.text =
            "Nama Biaya : ${data.biaya_nama}"
        binding.tvJumlahBiaya.text =
            "Jumlah (Quantity) : ${data.biaya_jumlah}"
        binding.tvPetani.text =
            "Petani : ${data.petani?.nama ?: "-"}"

        val targetLahanId = intent.getIntExtra("lahan_id", -1).takeIf { it != -1 }
        val targetLahanNama = intent.getStringExtra("lahan_nama")
        val nominalSplit = intent.getDoubleExtra("nominal_split", -1.0).takeIf { it != -1.0 }

        val listDetail = data.detail_biaya

        val selectedDetail = if (targetLahanId != null) {
            listDetail?.find { it.lahan?.id == targetLahanId }
        } else if (!targetLahanNama.isNullOrEmpty()) {
            listDetail?.find { it.lahan?.nama.equals(targetLahanNama, ignoreCase = true) }
        } else null

        if (selectedDetail != null || !targetLahanNama.isNullOrEmpty()) {
            val namaLahan = selectedDetail?.lahan?.nama ?: targetLahanNama ?: "-"
            val subtotalBiaya = selectedDetail?.subtotal ?: nominalSplit ?: 0.0
            val formattedTotal = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(subtotalBiaya.toLong())

            binding.tvTotalPengeluaran.text = "Total Pengeluaran : Rp $formattedTotal"
            binding.tvLahan.text = "Lahan : $namaLahan"
        } else {
            val formattedTotal = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(data.biaya_total.toLong())
            binding.tvTotalPengeluaran.text = "Total Pengeluaran : Rp $formattedTotal"

            val rincianLahanText = if (!listDetail.isNullOrEmpty()) {
                listDetail.mapIndexedNotNull { index, detail ->
                    val namaLahan = detail.lahan?.nama ?: return@mapIndexedNotNull null
                    val itemNama = detail.nama_detail.takeIf { !it.isNullOrEmpty() }
                    val subtotalVal = detail.subtotal
                    val subtotalStr = if (subtotalVal != null && subtotalVal > 0) {
                        val formatted = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(subtotalVal.toLong())
                        "Rp $formatted"
                    } else ""

                    val detailStr = when {
                        itemNama != null && subtotalStr.isNotEmpty() -> " : $itemNama ($subtotalStr)"
                        subtotalStr.isNotEmpty() -> " : $subtotalStr"
                        itemNama != null -> " : $itemNama"
                        else -> ""
                    }
                    "${index + 1}. $namaLahan$detailStr"
                }.joinToString("\n")
            } else {
                "-"
            }
            binding.tvLahan.text = "Rincian Lahan:\n$rincianLahanText"
        }

        binding.tvKeterangan.text =
            "Keterangan : ${data.biaya_ket ?: "-"}"

        val rawUrl = (data.biaya_bukti_url ?: data.biaya_bukti ?: "").trim()
        val fotoUrl = when {
            rawUrl.startsWith("http") -> rawUrl
            rawUrl.startsWith("storage/") -> "http://notasawit.pocari.id/$rawUrl"
            rawUrl.startsWith("biaya/") -> "http://notasawit.pocari.id/storage/$rawUrl"
            rawUrl.startsWith("/") -> "http://notasawit.pocari.id/storage$rawUrl"
            rawUrl.isNotEmpty() && rawUrl != "null" -> "http://notasawit.pocari.id/storage/biaya/$rawUrl"
            else -> ""
        }

        if (fotoUrl.isNotEmpty()) {
            Glide.with(this)
                .load(fotoUrl)
                .placeholder(R.drawable.ic_camera)
                .error(R.drawable.ic_camera)
                .into(binding.imgBuktiBiaya)

            binding.imgBuktiBiaya.setOnClickListener {
                com.example.notasawit.Utils.CustomAlert.showImagePreview(this, fotoUrl)
            }
        } else {
            binding.imgBuktiBiaya.setImageResource(R.drawable.ic_camera)
        }
    }
}