package com.example.notasawit.InputKegiatan

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.DetailKegiatan.DetailKegiatanEntity
import com.example.notasawit.Room.KegiatanPetani.KegiatanEntity
import com.example.notasawit.Room.Lahan.LahanEntity
import com.example.notasawit.Sync.SyncWorker
import com.example.notasawit.databinding.ActivityInputKegiatanBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class InputKegiatanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputKegiatanBinding
    private val listLahan = mutableListOf<LahanEntity>()
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE) }

    private val sp_petaniId by lazy {
        val id = sharedPref.getInt("petani_id", 0)
        if (id != 0) id else sharedPref.getInt("user_id", 0)
    }
    private val selectedLahanIds = mutableListOf<Int>()
    private val selectedLahanNames = mutableListOf<String>()
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
        
        setupDropdowns()
        loadLahan()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnRiwayat.setOnClickListener {
            startActivity(Intent(this, RiwayatKegiatanActivity::class.java))
        }

        binding.etPilihLahan.setOnClickListener {
            showLahanDialog()
        }

        binding.etTanggal.setOnClickListener {
            showDatePicker()
        }

        binding.btnSimpan.setOnClickListener {
            saveKegiatan()
        }
    }

    private fun setupDropdowns() {
        // Nama Kegiatan Dropdown (Pengendalian Hama, Pengendalian Gulma, Pemupukan)
        val kegiatanList = listOf("Pengendalian Hama", "Pengendalian Gulma", "Pemupukan")
        val kegiatanAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, kegiatanList)
        binding.spinnerKegiatan.setAdapter(kegiatanAdapter)
        binding.spinnerKegiatan.setOnClickListener { binding.spinnerKegiatan.showDropDown() }

        // Satuan Dropdown
        val satuanList = listOf("Liter", "Sak", "Kg", "Botol")
        val satuanAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, satuanList)
        binding.spinnerSatuan.setAdapter(satuanAdapter)
        binding.spinnerSatuan.setText("Liter", false)
        binding.spinnerSatuan.setOnClickListener { binding.spinnerSatuan.showDropDown() }

        // Jenis Limbah Dropdown
        val limbahList = listOf("Botol 1 Liter", "Botol 5 Liter", "Botol 20 Liter", "Plastik Pupuk", "Lainnya")
        val limbahAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, limbahList)
        binding.spinnerJenisLimbah.setAdapter(limbahAdapter)
        binding.spinnerJenisLimbah.setOnClickListener { binding.spinnerJenisLimbah.showDropDown() }
    }

    private fun saveKegiatan() {
        val tanggal = binding.etTanggal.tag?.toString() ?: binding.etTanggal.text.toString().trim()
        val namaKegiatan = binding.spinnerKegiatan.text.toString().trim()
        val namaBahan = binding.etNamaBahan.text.toString().trim()
        val dosisStr = binding.etDosis.text.toString().trim()
        val satuan = binding.spinnerSatuan.text.toString().trim().ifEmpty { "Liter" }
        val jenisLimbah = binding.spinnerJenisLimbah.text.toString().trim()
        val statusLimbah = if (binding.rbSudahDisetor.isChecked) "Sudah Disetor" else "Belum Disetor"

        if (tanggal.isEmpty()) {
            Toast.makeText(this, "Pilih tanggal rencana / pelaksanaan", Toast.LENGTH_SHORT).show()
            return
        }
        if (namaKegiatan.isEmpty()) {
            Toast.makeText(this, "Pilih nama kegiatan", Toast.LENGTH_SHORT).show()
            return
        }
        if (namaBahan.isEmpty()) {
            Toast.makeText(this, "Isi nama racun / pupuk", Toast.LENGTH_SHORT).show()
            return
        }
        if (dosisStr.isEmpty()) {
            Toast.makeText(this, "Isi jumlah yang digunakan", Toast.LENGTH_SHORT).show()
            return
        }
        if (jenisLimbah.isEmpty()) {
            Toast.makeText(this, "Pilih jenis limbah yang dihasilkan", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedLahanIds.isEmpty()) {
            Toast.makeText(this, "Pilih minimal satu lokasi lahan", Toast.LENGTH_SHORT).show()
            return
        }

        val dosis = dosisStr.toDoubleOrNull()?.toInt() ?: 1

        // Map nama kegiatan ke ID opsional jika backend memerlukan ID
        val jenisId = when (namaKegiatan) {
            "Pemupukan" -> 1
            "Pengendalian Gulma" -> 2
            "Pengendalian Hama" -> 3
            else -> 0
        }

        lifecycleScope.launch {
            val kegiatan = KegiatanEntity(
                kegiatan_tanggal = tanggal,
                kegiatan_jumlah = dosis,
                kegiatan_satuan = satuan,
                kegiatan_jenis = jenisId,
                nama_kegiatan = namaKegiatan,
                petani_id = sp_petaniId,
                kegiatan_ket = namaBahan,
                nama_bahan = namaBahan,
                jenis_limbah = jenisLimbah,
                status_limbah = statusLimbah,
                isSynced = false
            )

            // 1. Simpan ke database lokal Room
            val kegiatanId = database.KegiatanDao().insert(kegiatan)

            selectedLahanIds.forEach { lahanId ->
                database.DetailKegiatanDao().insert(
                    DetailKegiatanEntity(
                        kegiatanId = kegiatanId.toInt(),
                        lahanId = lahanId
                    )
                )
            }

            // 2. Picu WorkManager sinkronisasi ke server
            triggerDataSync()

            Toast.makeText(this@InputKegiatanActivity, "Kegiatan berhasil disimpan & mengantre sinkronisasi", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun showLahanDialog() {
        val items = listLahan.map { it.lahan_nama }.toTypedArray()
        val checked = BooleanArray(items.size)

        listLahan.forEachIndexed { index, lahan ->
            checked[index] = selectedLahanIds.contains(lahan.lahan_id)
        }

        AlertDialog.Builder(this)
            .setTitle("Pilih Lokasi Lahan Pengendalian")
            .setMultiChoiceItems(items, checked) { _, which, isChecked ->
                val lahan = listLahan[which]
                if (isChecked) {
                    if (!selectedLahanIds.contains(lahan.lahan_id)) {
                        selectedLahanIds.add(lahan.lahan_id)
                        selectedLahanNames.add(lahan.lahan_nama)
                    }
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
                            selectedLahanIds.removeAt(index)
                            selectedLahanNames.removeAt(index)
                            binding.chipGroupLahan.removeView(chip)
                        }
                    }
                    binding.chipGroupLahan.addView(chip)
                }
            }
            .setNegativeButton("Batal", null)
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

                val formatterUser = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
                binding.etTanggal.setText(formatterUser.format(selectedDate.time))

                val formatterServer = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val tanggalStandard = formatterServer.format(selectedDate.time)

                binding.etTanggal.tag = tanggalStandard
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun triggerDataSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "sync_data",
            ExistingWorkPolicy.KEEP,
            syncRequest
        )
    }

    override fun onResume() {
        super.onResume()
        triggerDataSync()
    }
}