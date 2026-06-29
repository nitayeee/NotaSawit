package com.example.notasawit.InputKegiatan

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.InputKegiatan.JenisKegiatan.JenisKegiatan
import com.example.notasawit.InputKegiatan.JenisKegiatan.JenisKegiatanApiResponse
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityInputKegiatanBinding
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import android.app.DatePickerDialog
import android.util.Log
import com.example.notasawit.Model.Lahan
import com.example.notasawit.Model.LahanResponse
import java.util.Locale

class InputKegiatanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputKegiatanBinding
    private val jenisKegiatanList = mutableListOf<JenisKegiatan>()
    private val listLahan = mutableListOf<Lahan>()
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE) }

    private val sp_petaniId by lazy { sharedPref.getInt("petani_id", 0) }
    private var selectedLahan: Int? = null
    private var selectedJKId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInputKegiatanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        loadJenisKegiatan()
        loadLahan()
        //Ambil ID Desa
        binding.spinnerKegiatan.setOnItemClickListener { _, _, position, _ ->

            selectedJKId = jenisKegiatanList[position].id_jenis
        }
        binding.spinnerLahan.setOnItemClickListener { _, _, position, _ ->

            selectedLahan = listLahan[position].lahan_id
        }
        binding.etTanggal.setOnClickListener {
            showDatePicker()
        }



    }
    private fun loadJenisKegiatan() {

        PetaniApi.getJenisKegiatan(object : Callback {

            override fun onFailure(
                call: Call,
                e: IOException
            ) {

                runOnUiThread {

                    Toast.makeText(
                        this@InputKegiatanActivity,
                        "Gagal mengambil jenis kegiatan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(
                call: Call,
                response: Response
            ) {

                val json = response.body?.string()

                val JKResponse =
                    Gson().fromJson(
                        json,
                        JenisKegiatanApiResponse::class.java
                    )

                jenisKegiatanList.clear()
                jenisKegiatanList.addAll(JKResponse.data)

                val namaDesa =
                    jenisKegiatanList.map { it.nama_jenis }

                runOnUiThread {

                    val adapter = ArrayAdapter(
                        this@InputKegiatanActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        namaDesa
                    )

                    binding.spinnerKegiatan.setAdapter(adapter)
                }
            }
        })
    }

    private fun loadLahan() {
        val petaniId = sp_petaniId
        PetaniApi.getLahanByPetani(
            petaniId,
            object : Callback {

                override fun onFailure(
                    call: Call,
                    e: IOException
                ) {

                    runOnUiThread {

                        Toast.makeText(
                            this@InputKegiatanActivity,
                            "Gagal mengambil jenis kegiatan",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onResponse(
                    call: Call,
                    response: Response
                ) {

                    val json = response.body?.string()

                    val JKResponse =
                        Gson().fromJson(
                            json,
                            LahanResponse::class.java
                        )

                    listLahan.clear()
                    listLahan.addAll(JKResponse.data)

                    val namaLahan =
                        listLahan.map { it.lahan_nama }

                    runOnUiThread {

                        val adapter = ArrayAdapter(
                            this@InputKegiatanActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            namaLahan
                        )

                        binding.spinnerLahan.setAdapter(adapter)
                    }
                }
            }
        )
    }

//    Untuk tanggal
private fun showDatePicker() {

    val calendar = Calendar.getInstance()

    val datePickerDialog = DatePickerDialog(
        this,
        { _, year, month, dayOfMonth ->

            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, dayOfMonth)

            val formatter = SimpleDateFormat(
                "dd MMM yyyy",
                Locale("id", "ID")
            )

            binding.etTanggal.setText(
                formatter.format(selectedDate.time)
            )
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    datePickerDialog.show()
}
}