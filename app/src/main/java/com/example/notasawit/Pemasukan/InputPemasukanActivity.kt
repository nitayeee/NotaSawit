package com.example.notasawit.Pemasukan

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ContentValues
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.Model.Lahan
import com.example.notasawit.Model.LahanResponse
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityInputPemasukanBinding
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InputPemasukanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityInputPemasukanBinding
    private val listLahan = mutableListOf<Lahan>()
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE) }
    private val sp_petaniId by lazy { sharedPref.getInt("petani_id", 0) }
    private var selectedLahan: Int? = null

    private var imageUri: Uri? = null
    private var cameraUri: Uri? = null

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
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { success ->
            if(success){
                cameraUri?.let {
                    imageUri = it
                    binding.imgNotaPreview.visibility = View.VISIBLE
                    binding.imgNotaPreview.setImageURI(it)

                }

            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        binding = ActivityInputPemasukanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )

            insets
        }

        // Klik upload bukti nota
        binding.cardUploadNota.setOnClickListener {
            showImagePickerDialog()
        }

        loadLahan()
        binding.spinnerLahan.setOnItemClickListener { _, _, position, _ ->
            selectedLahan = listLahan[position].lahan_id
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
            PetaniApi.postProduksi(
                context = this@InputPemasukanActivity,
                produksiTanggal = binding.etTanggal.text.toString(),
                jumlahTbs = binding.etBahan.text.toString().toInt(),
                hargaTbs = binding.etHarga.text.toString().toDouble(),
                petaniId = sp_petaniId,
                lahanId = selectedLahan ?: 0,
                produksiKet = binding.etCatatan.text.toString(),
                imageUri = imageUri,
                callback = object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(
                                this@InputPemasukanActivity,
                                "Gagal terhubung ke server: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {

                        val responseBody = response.body?.string() ?: ""

                        Log.d("PRODUKSI", "Code: ${response.code}")
                        Log.d("PRODUKSI", "Response: $responseBody")


                        this@InputPemasukanActivity.runOnUiThread {

                            if (response.isSuccessful) {

                                Toast.makeText(
                                    this@InputPemasukanActivity,
                                    "Produksi berhasil disimpan",
                                    Toast.LENGTH_SHORT
                                ).show()

                            } else {

                                Toast.makeText(
                                    this@InputPemasukanActivity,
                                    "Gagal simpan (${response.code})",
                                    Toast.LENGTH_LONG
                                ).show()

                            }
                        }
                    }

                }
            )

        }


        //Ambil ID Lahan


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
                        cameraUri = createImageUri()

                        cameraUri?.let {
                            cameraLauncher.launch(it)
                        }
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
                            this@InputPemasukanActivity,
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
                            this@InputPemasukanActivity,
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
    private fun createImageUri(): Uri? {

        val contentValues = ContentValues().apply {

            put(
                MediaStore.Images.Media.DISPLAY_NAME,
                "bukti_${System.currentTimeMillis()}.jpg"
            )

            put(
                MediaStore.Images.Media.MIME_TYPE,
                "image/jpeg"
            )
        }


        return contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
    }
}