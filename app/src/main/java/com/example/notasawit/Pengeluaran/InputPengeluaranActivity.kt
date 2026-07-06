package com.example.notasawit.Pengeluaran

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Model.Lahan
import com.example.notasawit.Model.LahanResponse
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.Repository.SyncProduksiRepository
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.Pengeluaran.PengeluaranEntity
import com.example.notasawit.Room.Produksi.ProduksiEntity
import com.example.notasawit.Sync.SyncPengeluaranRepository
import com.example.notasawit.Utils.NetworkUtil
import com.example.notasawit.databinding.ActivityInputPemasukanBinding
import com.example.notasawit.databinding.ActivityInputPengeluaranBinding
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
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
    private lateinit var database: AppDatabase

    // Pilih dari galeri
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            uri?.let {

                val localPath = copyImageToInternalStorage(it)

                imageUri = Uri.fromFile(File(localPath))

                binding.imgNotaPreview.visibility = View.VISIBLE
                binding.imgNotaPreview.setImageURI(imageUri)
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
        database = AppDatabase.getDatabase(this)
        // Klik upload bukti nota
        binding.cardUploadNota.setOnClickListener {
            showImagePickerDialog()
        }
        setupKategoriSpinner()

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

            lifecycleScope.launch {

                val pengeluaran = PengeluaranEntity(

                    biaya_tanggal = binding.etTanggal.text.toString(),

                    biaya_jenis = binding.spinnerKategori.text.toString(),

                    biaya_nama = binding.etNamaItem.text.toString(),

                    biaya_jumlah = binding.etJumlah.text.toString().toInt(),
                    petani_id = sp_petaniId,

                    lahan_id = selectedLahan!!,

                    biaya_total = binding.etTotal.text.toString().toDouble(),
                    biaya_ket = binding.etCatatan.text.toString(),

                    imagePath = imageUri?.toString()

                )

                database.PengeluaranDao().insert(pengeluaran)
                val semuaData = database.PengeluaranDao().getAll()

                Log.d("ROOM", "Jumlah data = ${semuaData.size}")

                semuaData.forEach {
                    Log.d("ROOM", it.toString())
                }

                Toast.makeText(
                    this@InputPengeluaranActivity,
                    "Data berhasil disimpan",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("SYNC_IMAGE", "imagePath = ${pengeluaran.imagePath}")

                if (NetworkUtil.isOnline(this@InputPengeluaranActivity)) {
                    Toast.makeText(
                        this@InputPengeluaranActivity,
                        "Connect ada",
                        Toast.LENGTH_SHORT
                    ).show()

                    SyncPengeluaranRepository(
                        this@InputPengeluaranActivity,
                        database
                    ).syncPengeluaran()

                }

                finish()

            }
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
    private fun setupKategoriSpinner() {
        val kategoriList = listOf(
            "Bibit",
            "Pupuk",
            "Pestisida",
            "Upah Pekerja",
            "Alat & Mesin",
            "Perbaikan",
            "BBM",
            "Transportasi",
            "Perlengkapan",
            "Operasional",
            "Pajak & Perizinan",
            "Sewa",
            "Lain-lain"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            kategoriList
        )

        binding.spinnerKategori.setAdapter(adapter)
    }
    private fun copyImageToInternalStorage(uri: Uri): String {

        val fileName = "notaPengeluaran_${System.currentTimeMillis()}.jpg"

        // Folder: files/nota/
        val dir = File(filesDir, "nota_pengeluaran")
        if (!dir.exists()) {
            dir.mkdirs()
        }

        // File tujuan
        val file = File(dir, fileName)

        contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }

        return file.absolutePath
    }
}