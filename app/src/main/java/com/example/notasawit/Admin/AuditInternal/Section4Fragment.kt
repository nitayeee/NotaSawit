package com.example.notasawit.Admin.AuditInternal

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notasawit.Admin.AuditInternal.AuditViewModel.AuditViewModel
import com.example.notasawit.Admin.AuditInternal.adapter.BuktiTemuanAdapter
import com.example.notasawit.Admin.AuditInternal.model.BuktiTemuanHelper
import com.example.notasawit.Admin.AuditInternal.model.BuktiTemuanItem
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.FragmentSection4Binding
import com.example.notasawit.Utils.CustomAlert
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.getValue

class Section4Fragment : Fragment() {

    private var _binding: FragmentSection4Binding? = null
    private val binding get() = _binding!!
    private val viewModel: AuditViewModel by activityViewModels()
    private lateinit var database: AppDatabase
    
    private val listBuktiTemuan = mutableListOf<BuktiTemuanItem>()
    private lateinit var buktiAdapter: BuktiTemuanAdapter

    private var tempCameraFilePath: String? = null

    // Launcher untuk kamera dengan resolusi penuh (Full Resolution via TakePicture)
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && !tempCameraFilePath.isNullOrEmpty()) {
            val file = File(tempCameraFilePath!!)
            if (file.exists() && file.length() > 0) {
                // Perbaiki rotasi EXIF agar foto tegak lurus sempurna
                fixImageOrientation(file.absolutePath)

                listBuktiTemuan.add(BuktiTemuanItem(fotoPath = file.absolutePath, keterangan = ""))
                buktiAdapter.notifyItemInserted(listBuktiTemuan.size - 1)
                binding.rvBuktiTemuan.scrollToPosition(listBuktiTemuan.size - 1)
            }
        }
    }

    // Launcher untuk galeri
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = saveUriToStorage(uri)
            if (path != null) {
                listBuktiTemuan.add(BuktiTemuanItem(fotoPath = path, keterangan = ""))
                buktiAdapter.notifyItemInserted(listBuktiTemuan.size - 1)
                binding.rvBuktiTemuan.scrollToPosition(listBuktiTemuan.size - 1)
            }
        }
    }

    private fun launchCamera() {
        try {
            val directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (directory != null && !directory.exists()) {
                directory.mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(directory, "Audit_Temuan_${timeStamp}_${System.currentTimeMillis() % 1000}.jpg")
            tempCameraFilePath = file.absolutePath
            val uri = androidx.core.content.FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUriToStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            if (directory != null && !directory.exists()) {
                directory.mkdirs()
            }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(directory, "Audit_Temuan_${timeStamp}_${System.currentTimeMillis() % 1000}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            // Perbaiki orientasi foto galeri jika ada rotasi EXIF
            fixImageOrientation(file.absolutePath)

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Gagal menyimpan foto dari galeri", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun fixImageOrientation(imagePath: String) {
        try {
            val exif = android.media.ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                android.media.ExifInterface.TAG_ORIENTATION,
                android.media.ExifInterface.ORIENTATION_NORMAL
            )
            val rotationAngle = when (orientation) {
                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90
                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180
                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            if (rotationAngle != 0) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(imagePath)
                if (bitmap != null) {
                    val matrix = android.graphics.Matrix().apply { postRotate(rotationAngle.toFloat()) }
                    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                    val out = FileOutputStream(imagePath)
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    out.flush()
                    out.close()
                    bitmap.recycle()
                    rotatedBitmap.recycle()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPickPhotoDialog() {
        val options = arrayOf("📷 Ambil Foto (Kamera)", "🖼️ Pilih dari Galeri")
        AlertDialog.Builder(requireContext())
            .setTitle("Tambah Bukti Foto Temuan")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchCamera()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSection4Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as AuditInternalActivity
        database = activity.database
        // Awal disabled
        binding.etTemuan.isEnabled = false
        binding.etPerbaikan.isEnabled = false

        setupRadio()
        setupDatePicker()
        binding.etTemuan.setText(viewModel.auditHeader.ringkasanTemuan)
        binding.etPerbaikan.setText(viewModel.auditHeader.rencanaPerbaikan)
        binding.etTanggalAudit.setText(viewModel.auditHeader.rencanaPemeriksaan)
        if (viewModel.auditHeader.ringkasanTemuan.isBlank()) {
            binding.rbTidakAdaTemuan.isChecked = true
        } else {
            binding.rbOtherTemuan.isChecked = true
            binding.etTemuan.isEnabled = true
        }

        // Setup RecyclerView Bukti Temuan
        listBuktiTemuan.clear()
        listBuktiTemuan.addAll(BuktiTemuanHelper.fromJson(viewModel.auditHeader.fotoPath))

        buktiAdapter = BuktiTemuanAdapter(listBuktiTemuan) { position ->
            if (position in 0 until listBuktiTemuan.size) {
                listBuktiTemuan.removeAt(position)
                buktiAdapter.notifyItemRemoved(position)
                buktiAdapter.notifyItemRangeChanged(position, listBuktiTemuan.size - position)
            }
        }
        binding.rvBuktiTemuan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBuktiTemuan.adapter = buktiAdapter

        binding.btnTambahFoto.setOnClickListener {
            showPickPhotoDialog()
        }

        binding.rgStatusAudit.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbStatusLulus.id) {
                binding.cardTanggalAudit.visibility = View.GONE
                binding.etTanggalAudit.setText("")
            } else {
                binding.cardTanggalAudit.visibility = View.VISIBLE
            }
        }

        if (viewModel.auditHeader.statusAudit == "Lulus") {
            binding.rbStatusLulus.isChecked = true
            binding.cardTanggalAudit.visibility = View.GONE
        } else {
            binding.rbStatusPerbaikan.isChecked = true
            binding.cardTanggalAudit.visibility = View.VISIBLE
        }

        binding.btnBack.setOnClickListener {
            (requireActivity() as AuditInternalActivity)
                .navigateBack(3)
        }

        binding.btnSelesai.setOnClickListener {
            if (!validasi()) return@setOnClickListener

            val selectedTemuanId = binding.rgTemuan.checkedRadioButtonId
            val temuanValue = if (selectedTemuanId == binding.rbOtherTemuan.id) {
                binding.etTemuan.text.toString()
            } else {
                view.findViewById<android.widget.RadioButton>(selectedTemuanId).text.toString()
            }

            val selectedPerbaikanId = binding.rgPerbaikan.checkedRadioButtonId
            val perbaikanValue = if (selectedPerbaikanId == binding.rbOtherPerbaikan.id) {
                binding.etPerbaikan.text.toString()
            } else {
                view.findViewById<android.widget.RadioButton>(selectedPerbaikanId).text.toString()
            }

            val statusAudit = if (binding.rbStatusLulus.isChecked) "Lulus" else "Perlu Perbaikan"
            val rencanaPemeriksaan = if (binding.rbStatusLulus.isChecked) "" else binding.etTanggalAudit.text.toString()
            val fotoPathJson = BuktiTemuanHelper.toJson(listBuktiTemuan)

            viewModel.auditHeader =
                viewModel.auditHeader.copy(
                    ringkasanTemuan = temuanValue,
                    rencanaPerbaikan = perbaikanValue,
                    rencanaPemeriksaan = rencanaPemeriksaan,
                    statusAudit = statusAudit,
                    fotoPath = fotoPathJson
                )

            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                // Generate PDF
                val generatedPdfPath = PdfGenerator.generatePdf(requireContext(), viewModel.auditHeader, viewModel.auditAnswers)
                if (generatedPdfPath != null) {
                    viewModel.auditHeader = viewModel.auditHeader.copy(pdfPath = generatedPdfPath)
                }

                // Simpanz Header
                database.auditDao().insertHeader(viewModel.auditHeader)
                
                // Simpan Answers
                val listAnswers = viewModel.auditAnswers.map { (key, value) ->
                    com.example.notasawit.Room.AuditEntity.AuditAnswer(
                        idAudit = viewModel.auditHeader.idAudit,
                        questionKey = key,
                        answer = value
                    )
                }
                database.auditDao().insertAnswers(listAnswers)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    triggerDataSync()
                    CustomAlert.showSuccess(
                        requireActivity(),
                        "Berhasil",
                        "Audit & PDF disimpan & siap disinkron!"
                    )
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        requireActivity().finish() // Tutup activity setelah berhasil simpan
                    }, 1500)
                }
            }
        }
    }

    private fun triggerDataSync() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
            .build()

        val syncRequest = androidx.work.OneTimeWorkRequestBuilder<com.example.notasawit.Sync.SyncWorker>()
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(requireContext()).enqueueUniqueWork(
            "SyncAuditWork",
            androidx.work.ExistingWorkPolicy.REPLACE,
            syncRequest
        )
    }

    private fun setupRadio() {
        binding.rgTemuan.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbOtherTemuan.id) {
                binding.etTemuan.isEnabled = true
            } else {
                binding.etTemuan.setText("")
                binding.etTemuan.isEnabled = false
            }
        }

        binding.rgPerbaikan.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbOtherPerbaikan.id) {
                binding.etPerbaikan.isEnabled = true
            } else {
                binding.etPerbaikan.setText("")
                binding.etPerbaikan.isEnabled = false
            }
        }
    }

    private fun setupDatePicker() {
        binding.etTanggalAudit.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    val format =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    binding.etTanggalAudit.setText(
                        format.format(calendar.time)
                    )
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun validasi(): Boolean {
        if (binding.rgTemuan.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(),"Pilih Ringkasan Temuan",Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.rbOtherTemuan.isChecked &&
            binding.etTemuan.text.isNullOrBlank()) {
            binding.etTemuan.error = "Harus diisi"
            return false
        }

        if (binding.rgPerbaikan.checkedRadioButtonId == -1) {
            Toast.makeText(requireContext(),"Pilih Rencana Perbaikan",Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.rbOtherPerbaikan.isChecked &&
            binding.etPerbaikan.text.isNullOrBlank()) {
            binding.etPerbaikan.error = "Harus diisi"
            return false
        }

        if (binding.rbStatusPerbaikan.isChecked) {
            if (binding.etTanggalAudit.text.isNullOrBlank()) {
                binding.etTanggalAudit.error = "Pilih tanggal"
                Toast.makeText(requireContext(), "Tanggal perbaikan selanjutnya wajib diisi!", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}