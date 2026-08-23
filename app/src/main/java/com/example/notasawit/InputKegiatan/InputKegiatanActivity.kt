package com.example.notasawit.InputKegiatan

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityInputKegiatanBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import android.app.DatePickerDialog
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.DetailKegiatan.DetailKegiatanEntity
import com.example.notasawit.Room.JenisKegiatan.JenisKegiatanEntity
import com.example.notasawit.Room.KegiatanPetani.KegiatanEntity
import com.example.notasawit.Room.Lahan.LahanEntity
import com.example.notasawit.Sync.SyncWorker
import kotlinx.coroutines.launch
import java.util.Locale

class InputKegiatanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputKegiatanBinding
    private val jenisKegiatanList = mutableListOf<JenisKegiatanEntity>()
    private val listLahan = mutableListOf<LahanEntity>()
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE) }

    private val sp_petaniId by lazy {
        val id = sharedPref.getInt("petani_id", 0)
        if (id != 0) id else sharedPref.getInt("user_id", 0)
    }
    private val selectedLahanIds = mutableListOf<Int>()
    private val selectedLahanNames = mutableListOf<String>()
    private var selectedJKId: Int? = null
    private lateinit var database: AppDatabase

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
        database = AppDatabase.getDatabase(this)
        loadJenisKegiatan()
        loadLahan()
        //Ambil ID Desa
        binding.spinnerKegiatan.setOnItemClickListener { _, _, position, _ ->

            selectedJKId = jenisKegiatanList[position].id_jenis
        }
        binding.btnBack.setOnClickListener { finish() }
//        binding.spinnerLahan.setOnItemClickListener { _, _, position, _ ->
//
//            selectedLahan = listLahan[position].lahan_id
//        }
        binding.etPilihLahan.setOnClickListener {
            showLahanDialog()
        }
        binding.etTanggal.setOnClickListener {
            showDatePicker()
        }
        binding.btnSimpan.setOnClickListener {
            lifecycleScope.launch {
                if (selectedLahanIds.isEmpty()) {
                    Toast.makeText(this@InputKegiatanActivity, "Pilih minimal satu lahan", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val kegiatan = KegiatanEntity(
                    kegiatan_tanggal = binding.etTanggal.tag?.toString() ?: binding.etTanggal.text.toString(),
                    kegiatan_jumlah = binding.etDosis.text.toString().toInt(),
                    kegiatan_satuan = "Kg",
                    kegiatan_jenis = selectedJKId!!,
                    petani_id = sp_petaniId,
                    kegiatan_ket = binding.etBahan.text.toString(),
                    isSynced = false // Data baru ditandai belum tersinkron
                )

                // 1. Selalu simpan ke database lokal terlebih dahulu (Aman & Pasti tersimpan)
                val kegiatanId = database.KegiatanDao().insert(kegiatan)

                selectedLahanIds.forEach { lahanId ->
                    database.DetailKegiatanDao().insert(
                        DetailKegiatanEntity(
                            kegiatanId = kegiatanId.toInt(),
                            lahanId  = lahanId
                        )
                    )
                }

                // 2. Picu WorkManager untuk menangani pengiriman data & pembersihan lokal
                triggerDataSync()

                Toast.makeText(this@InputKegiatanActivity, "Data disimpan ke lokal & mengantre sinkronisasi", Toast.LENGTH_SHORT).show()
                finish()
            }
        }



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
    private fun loadJenisKegiatan() {

        lifecycleScope.launch {

            jenisKegiatanList.clear()
            jenisKegiatanList.addAll(
                database.JenisKegiatanDao().getAllJenisKegiatan()
            )

            val adapter = ArrayAdapter(
                this@InputKegiatanActivity,
                android.R.layout.simple_dropdown_item_1line,
                jenisKegiatanList.map { it.nama_jenis }
            )

            binding.spinnerKegiatan.setAdapter(adapter)
        }
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

//    Untuk tanggal
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
    private fun triggerDataSync() {
        // Buat aturan wajib: Harus terhubung internet
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        // Jalankan antrean background task
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "SyncKegiatanWork",
            ExistingWorkPolicy.KEEP, // Tetap jalankan antrean yang ada, jangan ditimpa/dihapus
            syncRequest
        )
    }

    override fun onResume() {
        super.onResume()
        // Saat aplikasi dibuka kembali, cek apakah ada data tertinggal yang perlu disinkronkan
        triggerDataSync()
    }
}