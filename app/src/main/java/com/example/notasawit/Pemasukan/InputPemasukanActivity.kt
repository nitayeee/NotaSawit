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
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.DetailKegiatan.DetailKegiatanEntity
import com.example.notasawit.Room.DetailProduksi.DetailProduksiEntity
import com.example.notasawit.Room.Lahan.LahanEntity
import com.example.notasawit.Room.Produksi.ProduksiEntity
import com.example.notasawit.Sync.SyncKegiatanRepository
import com.example.notasawit.Utils.NetworkUtil
import com.example.notasawit.databinding.ActivityInputPemasukanBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InputPemasukanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputPemasukanBinding
    private val listLahan = mutableListOf<LahanEntity>()
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE) }
    private val sp_petaniId by lazy { sharedPref.getInt("petani_id", 0) }
    private val selectedLahanIds = mutableListOf<Int>()
    private val selectedLahanNames = mutableListOf<String>()
    private var imageUri: Uri? = null
    private var cameraUri: Uri? = null
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
        database = AppDatabase.getDatabase(this)

        // Klik upload bukti nota
        binding.cardUploadNota.setOnClickListener {
            showImagePickerDialog()
        }

        loadLahan()
        binding.etPilihLahan.setOnClickListener {
            showLahanDialog()
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
            lifecycleScope.launch {

                val produksi = ProduksiEntity(

                    produksi_tanggal = binding.etTanggal.text.toString(),

                    jumlah_tbs = binding.etBahan.text.toString().toInt(),

                    harga_tbs = binding.etHarga.text.toString().toDouble(),
                    total_pendapatan = binding.etBahan.text.toString().toInt() *
                            binding.etHarga.text.toString().toDouble(),

                    petaniId = sp_petaniId,

                    produksi_ket = binding.etCatatan.text.toString(),

                    imagePath = imageUri?.toString()

                )
                val produksiId = database.ProduksiDao().insert(produksi)

                // Simpan semua lahan yang dipilih
                selectedLahanIds.forEach { lahanId ->

                    database.DetailProduksiDao().insert(

                        DetailProduksiEntity(
                            produksiId = produksiId.toInt(),
                            lahanId  = lahanId
                        )

                    )

                }
                if (NetworkUtil.isOnline(this@InputPemasukanActivity)) {

                    SyncKegiatanRepository(
                        this@InputPemasukanActivity,
                        database
                    ).sync()

                }


//                database.ProduksiDao().insert(produksi)
//                val semuaData = database.ProduksiDao().getAll()
//
//                Log.d("ROOM", "Jumlah data = ${semuaData.size}")
//
//                semuaData.forEach {
//                    Log.d("ROOM", it.toString())
//                }

                Toast.makeText(
                    this@InputPemasukanActivity,
                    "Data berhasil disimpan",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("SYNC_IMAGE", "imagePath = ${produksi.imagePath}")

//                if (NetworkUtil.isOnline(this@InputPemasukanActivity)) {
//
//                    SyncProduksiRepository(
//                        this@InputPemasukanActivity,
//                        database
//                    ).syncProduksi()
//
//                }

                finish()

            }

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

        lifecycleScope.launch {

            listLahan.clear()
            listLahan.addAll(
                database.LahanDao().getAllLahan()
            )

        }
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
    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
//            if (NetworkUtil.isOnline(this@InputPemasukanActivity)) {
//                SyncProduksiRepository(
//                    this@InputPemasukanActivity,
//                    database
//                ).syncProduksi()
//            }
        }
    }
    private fun copyImageToInternalStorage(uri: Uri): String {

        val fileName = "nota_${System.currentTimeMillis()}.jpg"

        // Folder: files/nota/
        val dir = File(filesDir, "nota_pemasukan")
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
    private fun showLahanDialog() {

        val items = listLahan.map { it.lahan_nama }.toTypedArray()
        val checked = BooleanArray(items.size)

        //menandai yang sudah dipilih
        listLahan.forEachIndexed { index, lahan ->
            checked[index] = selectedLahanIds.contains(lahan.lahan_id)
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Pilih Lahan")
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->

                val lahan = listLahan[which]

                if (isChecked) {

                    selectedLahanIds.add(lahan.lahan_id)
                    selectedLahanNames.add(lahan.lahan_nama)

                } else {

                    selectedLahanIds.remove(lahan.lahan_id)
                    selectedLahanNames.remove(lahan.lahan_nama)

                }

            }
            .setPositiveButton("Simpan") { _, _ ->

                binding.chipGroupLahan.removeAllViews()

                selectedLahanNames.forEach { nama ->

                    val chip = com.google.android.material.chip.Chip(this)
                    chip.text = nama
                    chip.isCloseIconVisible = true

                    chip.setOnCloseIconClickListener {

                        val index = selectedLahanNames.indexOf(nama)

                        if (index != -1) {
                            selectedLahanNames.removeAt(index)
                            selectedLahanIds.removeAt(index)
                        }

                        binding.chipGroupLahan.removeView(chip)
                    }

                    binding.chipGroupLahan.addView(chip)
                }

            }
            .setNegativeButton("Batal", null)
            .show()

    }
}