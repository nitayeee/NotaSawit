package com.example.notasawit.Admin.AuditInternal

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Admin.AuditInternal.AuditViewModel.AuditViewModel
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.FragmentSection4Binding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.getValue


class Section4Fragment : Fragment() {

    private var _binding: FragmentSection4Binding? = null
    private val binding get() = _binding!!
    private val viewModel: AuditViewModel by activityViewModels()
    private lateinit var database: AppDatabase
    
    private var currentPhotoPath: String? = null

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val directory = requireContext().getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                if (directory != null && !directory.exists()) {
                    directory.mkdirs()
                }
                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())
                val file = java.io.File(directory, "Audit_Bukti_$timeStamp.jpg")
                val outputStream = java.io.FileOutputStream(file)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                
                currentPhotoPath = file.absolutePath
                viewModel.auditForm = viewModel.auditForm.copy(fotoPath = currentPhotoPath ?: "")
                
                com.bumptech.glide.Glide.with(this)
                    .load(file)
                    .into(binding.ivBuktiAudit)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "Gagal menyimpan foto", Toast.LENGTH_SHORT).show()
            }
        }
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
        binding.etTemuan.setText(viewModel.auditForm.ringkasanTemuan)
        binding.etPerbaikan.setText(viewModel.auditForm.rencanaPerbaikan)
        binding.etTanggalAudit.setText(viewModel.auditForm.rencanaPemeriksaan)
        if (viewModel.auditForm.ringkasanTemuan.isBlank()) {
            binding.rbTidakAdaTemuan.isChecked = true
        } else {
            binding.rbOtherTemuan.isChecked = true
            binding.etTemuan.isEnabled = true
        }
        
        currentPhotoPath = viewModel.auditForm.fotoPath
        if (!currentPhotoPath.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(this)
                .load(java.io.File(currentPhotoPath!!))
                .into(binding.ivBuktiAudit)
        }

        binding.btnPilihFoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
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

            viewModel.auditForm =
                viewModel.auditForm.copy(
                    ringkasanTemuan = temuanValue,
                    rencanaPerbaikan = perbaikanValue,
                    rencanaPemeriksaan = binding.etTanggalAudit.text.toString()
                )

            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                // Generate PDF
                val generatedPdfPath = PdfGenerator.generatePdf(requireContext(), viewModel.auditForm)
                if (generatedPdfPath != null) {
                    viewModel.auditForm = viewModel.auditForm.copy(pdfPath = generatedPdfPath)
                }

                database.auditDao().insert(viewModel.auditForm)
                
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    triggerDataSync()
                    com.example.notasawit.utils.CustomAlert.showSuccess(
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

        if (binding.etTanggalAudit.text.isNullOrBlank()) {
            binding.etTanggalAudit.error = "Pilih tanggal"
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}