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
        binding.tvJumlahTbs.text =
            "Jumlah TBS : ${data.jumlah_tbs} Kg"
        binding.tvHargaTbs.text =
            "Harga TBS : Rp ${data.harga_tbs}"
        binding.tvTotalPendapatan.text =
            "Total Pendapatan : Rp ${data.total_pendapatan}"
        binding.tvStatus.text =
            "Status : ${data.status_validasi}"
        binding.tvPetani.text =
            "Petani : ${data.petani?.nama}"

        // PERBAIKAN: Mengambil nama-nama lahan dari list detail_produksi
        val daftarLahan = data.detail_produksi
            ?.mapNotNull { it.lahan?.nama } // Ambil nama lahan yang tidak null
            ?.distinct()                     // Hilangkan duplikat jika lahannya sama
            ?.joinToString(", ")            // Gabungkan dengan koma, contoh: "Lahan A, Lahan B"

        binding.tvLahan.text =
            "Lahan : ${if (!daftarLahan.isNullOrEmpty()) daftarLahan else "-"}"

        binding.tvKeterangan.text =
            "Keterangan : ${data.produksi_ket ?: "-"}"

        Glide.with(this)
            .load(data.produksi_bukti_url)
            .into(binding.imgBuktiProduksi)
    }
}