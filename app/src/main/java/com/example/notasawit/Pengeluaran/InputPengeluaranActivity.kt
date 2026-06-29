package com.example.notasawit.Pengeluaran

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.Model.Lahan
import com.example.notasawit.Model.LahanResponse
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityInputPemasukanBinding
import com.example.notasawit.databinding.ActivityInputPengeluaranBinding
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InputPengeluaranActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputPengeluaranBinding
    private val listLahan = mutableListOf<Lahan>()
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE) }

    private val sp_petaniId by lazy { sharedPref.getInt("petani_id", 0) }
    private var selectedLahan: Int? = null

    // Untuk Nota
    private var imageUri: Uri? = null

    // Pilih dari galeri
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            uri?.let {
                imageUri = it

                binding.imgNotaPreview.visibility = View.VISIBLE
                binding.imgNotaPreview.setImageURI(it)
            }
        }

    // Ambil foto dari kamera
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->

            bitmap?.let {
                binding.imgNotaPreview.visibility = View.VISIBLE
                binding.imgNotaPreview.setImageBitmap(it)
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityInputPengeluaranBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Klik upload bukti nota
        binding.cardUploadNota.setOnClickListener {
            showImagePickerDialog()
        }

        // Tombol simpan
        binding.btnSimpan.setOnClickListener {

            if (binding.imgNotaPreview.visibility == View.GONE) {

                AlertDialog.Builder(this)
                    .setTitle("Peringatan")
                    .setMessage("Silakan upload bukti nota terlebih dahulu")
                    .setPositiveButton("OK", null)
                    .show()

                return@setOnClickListener
            }

            // TODO: Simpan data pemasukan ke API
        }

        loadLahan()
        //Ambil ID Lahan

        binding.spinnerLahan.setOnItemClickListener { _, _, position, _ ->

            selectedLahan = listLahan[position].lahan_id
        }
        // Untuk tanggal
        binding.etTanggal.setOnClickListener {
            showDatePicker()
        }
    }
    private fun showImagePickerDialog() {

        val options = arrayOf(
            "📷 Ambil Foto",
            "🖼️ Pilih dari Galeri"
        )

        AlertDialog.Builder(this)
            .setTitle("Pilih Bukti Nota")
            .setItems(options) { _, which ->

                when (which) {

                    0 -> {
                        cameraLauncher.launch()
                    }

                    1 -> {
                        galleryLauncher.launch("image/*")
                    }
                }
            }
            .show()
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
                            this@InputPengeluaranActivity,
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
                            this@InputPengeluaranActivity,
                            android.R.layout.simple_dropdown_item_1line,
                            namaLahan
                        )

                        binding.spinnerLahan.setAdapter(adapter)
                    }
                }
            }
        )
    }
    private fun showDatePicker() {

        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->

                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)

                val formatter = SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.getDefault()
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