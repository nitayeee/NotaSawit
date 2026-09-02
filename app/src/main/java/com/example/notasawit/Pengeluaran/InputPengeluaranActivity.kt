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
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.DetailPengeluaran.DetailPengeluaranEntity
import com.example.notasawit.Room.Lahan.LahanEntity
import com.example.notasawit.Room.Pengeluaran.PengeluaranEntity
import com.example.notasawit.Sync.SyncPengeluaranRepository
import com.example.notasawit.Sync.SyncWorker
import com.example.notasawit.databinding.ActivityInputPengeluaranBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InputPengeluaranActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputPengeluaranBinding
    private val listLahan = mutableListOf<LahanEntity>()
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE) }

    private val sp_petaniId by lazy {
        val id = sharedPref.getInt("petani_id", 0)
        if (id != 0) id else sharedPref.getInt("user_id", 0)
    }
    private var selectedLahan: Int? = null
    private val selectedLahanIds = mutableListOf<Int>()
    private val selectedLahanNames = mutableListOf<String>()
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
        binding.etPilihLahan.setOnClickListener {
            showLahanDialog()
        }
        setupKategoriSpinner()
        binding.btnBack.setOnClickListener { finish() }

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

            if (selectedLahanIds.isEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Peringatan")
                    .setMessage("Silakan pilih minimal 1 Lahan")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }

            if (binding.etNamaItem.text.toString().isEmpty() || binding.etJumlah.text.toString().isEmpty() || binding.etTotal.text.toString().isEmpty() || binding.etTanggal.text.toString().isEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Peringatan")
                    .setMessage("Silakan lengkapi semua data")
                    .setPositiveButton("OK", null)
                    .show()
                return@setOnClickListener
            }

            lifecycleScope.launch {

                val biaya_operasional = PengeluaranEntity(

                    biaya_tanggal = binding.etTanggal.tag?.toString() ?: binding.etTanggal.text.toString(),

                    biaya_jenis = binding.spinnerKategori.text.toString(),

                    biaya_nama = binding.etNamaItem.text.toString(),

                    biaya_jumlah = binding.etJumlah.text.toString().toInt(),
                    petani_id = sp_petaniId,

                    biaya_total = binding.etTotal.text.toString().toDouble(),
                    biaya_ket = binding.etCatatan.text.toString(),

                    imagePath = imageUri?.toString(),
                    isSynced = false

                )
                val biaya_operasional_id = database.PengeluaranDao().insert(biaya_operasional)

                // Hitung pengeluaran proporsional per-lahan (sama seperti Pemasukan)
                val lahanTerpilih = listLahan.filter { selectedLahanIds.contains(it.lahan_id) }
                val totalLuasLahan = lahanTerpilih.sumOf { it.lahan_luas }
                val countLahan = lahanTerpilih.size
                val totalPengeluaran = biaya_operasional.biaya_total

                val pengeluaranPerHektar = if (totalLuasLahan > 0) totalPengeluaran / totalLuasLahan else 0.0
                val pengeluaranRata = if (countLahan > 0) totalPengeluaran / countLahan else 0.0

                var totalPengeluaranDihitung = 0.0

                lahanTerpilih.forEachIndexed { index, lahan ->
                    val subtotalLahan: Double

                    if (index == lahanTerpilih.lastIndex) {
                        subtotalLahan = totalPengeluaran - totalPengeluaranDihitung
                    } else {
                        if (totalLuasLahan > 0) {
                            subtotalLahan = lahan.lahan_luas * pengeluaranPerHektar
                        } else {
                            subtotalLahan = pengeluaranRata
                        }
                    }

                    totalPengeluaranDihitung += subtotalLahan

                    database.DetailPengeluaranDao().insert(
                        DetailPengeluaranEntity(
                            biaya_operasional_id = biaya_operasional_id.toInt(),
                            lahanId  = lahan.lahan_id,
                            subtotal = subtotalLahan
                        )
                    )
                }
                triggerDataSync()

                com.example.notasawit.Utils.CustomAlert.showSuccess(
                    this@InputPengeluaranActivity,
                    "Berhasil",
                    "Data disimpan & siap disinkron"
                )
                Log.d("SYNC_IMAGE", "imagePath = ${biaya_operasional.imagePath}")

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    finish()
                }, 1500)

            }

        }

        loadLahan()
        //Ambil ID Lahan

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
        lifecycleScope.launch {
            val currentPetaniId = sp_petaniId
            val allLahan = database.LahanDao().getAllLahan()
            val filteredLahan = if (currentPetaniId != 0) {
                val list = allLahan.filter { it.petani_id == currentPetaniId }
                if (list.isNotEmpty()) list else allLahan
            } else {
                allLahan
            }

            listLahan.clear()
            listLahan.addAll(filteredLahan)
        }
    }
    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth)

                // 1. Tampilan untuk USER di aplikasi (Tetap Bahasa Indonesia agar mudah dibaca)
                val formatterUser = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                binding.etTanggal.setText(formatterUser.format(selectedDate.time))

                // 2. Simpan format standar SERVER (yyyy-MM-dd) di tag/variabel tersembunyi
                val formatterServer = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val tanggalStandard = formatterServer.format(selectedDate.time)

                // Simpan tanggal standard ke dalam tag EditText agar bisa diambil nanti
                binding.etTanggal.tag = tanggalStandard
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
    private fun triggerDataSync() {
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            try {
                val success = SyncPengeluaranRepository(applicationContext, database).syncPengeluaran()
                Log.d("SYNC_DIRECT", "Pengeluaran direct sync success: $success")
            } catch (e: Exception) {
                Log.e("SYNC_DIRECT", "Pengeluaran direct sync failed, background worker will retry", e)
            }
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        // 🔄 Ganti KEEP menjadi REPLACE
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "SyncPengeluaranWork",
            ExistingWorkPolicy.REPLACE, // Mengganti antrean lama agar Worker langsung merespon data baru
            syncRequest
        )
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