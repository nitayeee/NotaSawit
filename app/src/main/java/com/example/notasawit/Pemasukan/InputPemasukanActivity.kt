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
import com.example.notasawit.Model.Lahan
import com.example.notasawit.Model.LahanResponse
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.Repository.SyncProduksiRepository
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.Lahan.LahanEntity
import com.example.notasawit.Room.Produksi.ProduksiEntity
import com.example.notasawit.Utils.NetworkUtil
import com.example.notasawit.databinding.ActivityInputPemasukanBinding
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

class InputPemasukanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputPemasukanBinding
    private val listLahan = mutableListOf<LahanEntity>()
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE) }
    private val sp_petaniId by lazy { sharedPref.getInt("petani_id", 0) }
    private var selectedLahan: Int? = null
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
            lifecycleScope.launch {

                val produksi = ProduksiEntity(

                    tanggal = binding.etTanggal.text.toString(),

                    jumlahTbs = binding.etBahan.text.toString().toInt(),

                    hargaTbs = binding.etHarga.text.toString().toDouble(),

                    petaniId = sp_petaniId,

                    lahanId = selectedLahan!!,

                    catatan = binding.etCatatan.text.toString(),

                    imagePath = imageUri?.toString()

                )

                database.ProduksiDao().insert(produksi)
                val semuaData = database.ProduksiDao().getAll()

                Log.d("ROOM", "Jumlah data = ${semuaData.size}")

                semuaData.forEach {
                    Log.d("ROOM", it.toString())
                }

                Toast.makeText(
                    this@InputPemasukanActivity,
                    "Data berhasil disimpan",
                    Toast.LENGTH_SHORT
                ).show()
                Log.d("SYNC_IMAGE", "imagePath = ${produksi.imagePath}")

                if (NetworkUtil.isOnline(this@InputPemasukanActivity)) {

                    SyncProduksiRepository(
                        this@InputPemasukanActivity,
                        database
                    ).syncProduksi()

                }

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

            val adapter = ArrayAdapter(
                this@InputPemasukanActivity,
                android.R.layout.simple_dropdown_item_1line,
                listLahan.map { it.lahan_nama }
            )

            binding.spinnerLahan.setAdapter(adapter)
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
            if (NetworkUtil.isOnline(this@InputPemasukanActivity)) {
                SyncProduksiRepository(
                    this@InputPemasukanActivity,
                    database
                ).syncProduksi()
            }
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
}