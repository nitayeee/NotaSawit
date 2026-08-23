package com.example.notasawit.Admin.KunjunganLahan

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.FragmentKlSection3Binding
import com.example.notasawit.Utils.CustomAlert
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KLSection3Fragment : Fragment() {

    private var _binding: FragmentKlSection3Binding? = null
    private val binding get() = _binding!!
    private val viewModel: KunjunganLahanViewModel by activityViewModels()

    private lateinit var database: AppDatabase
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentPhotoPath: String? = null
    private var photoUri: Uri? = null

    private var capturedLatitude: Double? = null
    private var capturedLongitude: Double? = null
    private var capturedWaktu: String? = null

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            currentPhotoPath?.let { path ->
                Glide.with(this).load(File(path)).into(binding.ivFotoBukti)
            }
        } else {
            Toast.makeText(requireContext(), "Batal mengambil foto", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        
        if (cameraGranted) {
            // Camera ready
        }
        if (locationGranted) {
            // Location ready
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKlSection3Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as KunjunganLahanActivity
        database = activity.database
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        requestPermissions()

        binding.etTanggalPerbaikan.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            android.app.DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                    binding.etTanggalPerbaikan.setText(format.format(calendar.time))
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.rgStatus.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbSelesai.id) {
                binding.layoutTanggalPerbaikan.visibility = View.GONE
                binding.etTanggalPerbaikan.setText("")
            } else {
                binding.layoutTanggalPerbaikan.visibility = View.VISIBLE
            }
        }

        if (viewModel.kunjunganLahanForm.statusKunjungan == "Lulus" || viewModel.kunjunganLahanForm.statusKunjungan == "Selesai") {
            binding.rbSelesai.isChecked = true
            binding.layoutTanggalPerbaikan.visibility = View.GONE
        } else {
            binding.rbPerluPerbaikan.isChecked = true
            binding.layoutTanggalPerbaikan.visibility = View.VISIBLE
        }

        binding.btnAmbilFoto.setOnClickListener {
            if (hasCameraPermission()) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(requireContext(), "Izin kamera dibutuhkan", Toast.LENGTH_SHORT).show()
                requestPermissions()
            }
        }

        binding.btnGetLokasiWaktu.setOnClickListener {
            if (hasLocationPermission()) {
                fetchLocationAndTime()
            } else {
                Toast.makeText(requireContext(), "Izin lokasi dibutuhkan", Toast.LENGTH_SHORT).show()
                requestPermissions()
            }
        }

        binding.btnSimpan.setOnClickListener {
            if (currentPhotoPath == null) {
                Toast.makeText(requireContext(), "Silakan ambil foto bukti terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (capturedLatitude == null || capturedLongitude == null) {
                Toast.makeText(requireContext(), "Silakan dapatkan lokasi dan waktu terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val statusKunjungan = if (binding.rbPerluPerbaikan.isChecked) "Perlu Perbaikan" else "Lulus"

            if (binding.rbPerluPerbaikan.isChecked && binding.etTanggalPerbaikan.text.isNullOrBlank()) {
                binding.etTanggalPerbaikan.error = "Pilih tanggal"
                Toast.makeText(requireContext(), "Tanggal perbaikan selanjutnya wajib diisi!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var temuan = binding.etRingkasanTemuan.text.toString().trim()
            if (binding.rbPerluPerbaikan.isChecked && !binding.etTanggalPerbaikan.text.isNullOrBlank()) {
                val tglPerbaikan = binding.etTanggalPerbaikan.text.toString().trim()
                if (!temuan.contains("Rencana Perbaikan:")) {
                    temuan = if (temuan.isEmpty()) "Rencana Perbaikan: $tglPerbaikan" else "$temuan\nRencana Perbaikan: $tglPerbaikan"
                }
            }

            viewModel.kunjunganLahanForm = viewModel.kunjunganLahanForm.copy(
                fotoBuktiPath = currentPhotoPath,
                latitude = capturedLatitude,
                longitude = capturedLongitude,
                waktuBukti = capturedWaktu,
                statusKunjungan = statusKunjungan,
                ringkasanTemuan = temuan
            )

            simpanKeDatabase()
        }

        binding.btnBack.setOnClickListener {
            (requireActivity() as KunjunganLahanActivity).navigateBack(2)
        }
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun dispatchTakePictureIntent() {
        val photoFile = try {
            createImageFile()
        } catch (ex: Exception) {
            Toast.makeText(requireContext(), "Gagal membuat file gambar", Toast.LENGTH_SHORT).show()
            null
        }

        photoFile?.also {
            photoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                it
            )
            takePictureLauncher.launch(photoUri)
        }
    }

    @Throws(Exception::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchLocationAndTime() {
        Toast.makeText(requireContext(), "Mengambil lokasi...", Toast.LENGTH_SHORT).show()
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val currentDateAndTime = sdf.format(Date())

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                capturedLatitude = location.latitude
                capturedLongitude = location.longitude
                capturedWaktu = currentDateAndTime

                binding.tvLokasi.text = "Lokasi: ${location.latitude}, ${location.longitude}"
                binding.tvWaktu.text = "Waktu: $currentDateAndTime"
            } else {
                // Jika lastLocation null (cache kosong), minta lokasi terbaru langsung dari GPS
                val cancellationTokenSource = com.google.android.gms.tasks.CancellationTokenSource()
                fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationTokenSource.token
                ).addOnSuccessListener { freshLocation: Location? ->
                    if (freshLocation != null) {
                        capturedLatitude = freshLocation.latitude
                        capturedLongitude = freshLocation.longitude
                        capturedWaktu = currentDateAndTime

                        binding.tvLokasi.text = "Lokasi: ${freshLocation.latitude}, ${freshLocation.longitude}"
                        binding.tvWaktu.text = "Waktu: $currentDateAndTime"
                    } else {
                        // Fallback jika belum mendapat sinyal satelit GPS (misal di emulator/dalam ruangan)
                        capturedLatitude = 0.0
                        capturedLongitude = 0.0
                        capturedWaktu = currentDateAndTime

                        binding.tvLokasi.text = "Lokasi: 0.0, 0.0 (GPS belum terdeteksi)"
                        binding.tvWaktu.text = "Waktu: $currentDateAndTime"
                        Toast.makeText(requireContext(), "Waktu berhasil dicatat. Pastikan GPS HP aktif.", Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener {
                    capturedLatitude = 0.0
                    capturedLongitude = 0.0
                    capturedWaktu = currentDateAndTime

                    binding.tvLokasi.text = "Lokasi: 0.0, 0.0"
                    binding.tvWaktu.text = "Waktu: $currentDateAndTime"
                }
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error mengambil lokasi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun simpanKeDatabase() {
        lifecycleScope.launch(Dispatchers.IO) {
            // Generate PDF
            val generatedPdfPath = PdfGeneratorKunjungan.generatePdf(requireContext(), viewModel.kunjunganLahanForm, viewModel.section2Answers)
            if (generatedPdfPath != null) {
                viewModel.kunjunganLahanForm = viewModel.kunjunganLahanForm.copy(pdfPath = generatedPdfPath)
            }

            database.KunjunganLahanDao().insertKunjunganLahan(viewModel.kunjunganLahanForm)
            withContext(Dispatchers.Main) {
                triggerDataSync()
                CustomAlert.showSuccess(
                    requireActivity(),
                    "Berhasil",
                    "Data & PDF disimpan & siap disinkron!"
                )
                Handler(Looper.getMainLooper()).postDelayed({
                    requireActivity().finish() // Tutup activity setelah berhasil simpan
                }, 1500)
            }
        }
    }

    private fun triggerDataSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = OneTimeWorkRequestBuilder<com.example.notasawit.Sync.SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniqueWork(
            "SyncKunjunganWork",
            ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
