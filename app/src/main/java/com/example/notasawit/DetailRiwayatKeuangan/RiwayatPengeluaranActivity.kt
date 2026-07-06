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
        val biaya_bukti_url: String?
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
        binding.tvTotalPengeluaran.text =
            "Total Pengeluaran : Rp ${data.biaya_total}"
        binding.tvPetani.text =
            "Petani : ${data.petani?.nama}"
        binding.tvLahan.text =
            "Lahan : ${data.lahan?.nama}"
        binding.tvKeterangan.text =
            "Keterangan : ${data.biaya_ket ?: "-"}"
        Glide.with(this)
            .load(data.biaya_bukti_url)
            .into(binding.imgBuktiBiaya)

    }

}