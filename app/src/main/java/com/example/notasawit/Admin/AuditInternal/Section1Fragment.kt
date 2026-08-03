package com.example.notasawit.Admin.AuditInternal

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.Auditor.AuditorEntity
import com.example.notasawit.Room.Petani.PetaniEntity
import com.example.notasawit.databinding.FragmentSection1Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.fragment.app.activityViewModels
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.notasawit.Admin.AuditInternal.AuditViewModel.AuditViewModel


class Section1Fragment : Fragment() {

    private var _binding: FragmentSection1Binding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private val viewModel: AuditViewModel by activityViewModels()
    
    private var listPetaniEntity: List<PetaniEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSection1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Ambil instansiasi database dari Activity
        val activity = requireActivity() as AuditInternalActivity
        database = activity.database

        // 2. Isi data dummy ke Room & tampilkan ke Dropdown Pencarian
        siapkanDanTampilkanDataMaster()
        // Tampilkan data yang sudah pernah disimpan
        binding.etTanggal.setText(viewModel.auditHeader.tanggal)

        // Setup DatePicker agar format sesuai dengan yang diharapkan server (yyyy-MM-dd)
        binding.etTanggal.isFocusable = false
        binding.etTanggal.isClickable = true
        binding.etTanggal.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(requireContext(), { _, y, m, d ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(y, m, d)
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                binding.etTanggal.setText(format.format(selectedDate.time))
            }, year, month, day)
            dpd.show()
        }
        binding.acDesa.setText(viewModel.auditHeader.desa, false)
        binding.etAuditor.setText(viewModel.auditHeader.namaAuditor)
        binding.etPetani.setText(viewModel.auditHeader.namaPetani)

        // 3. Logika tombol Lanjut ke Section 2
        binding.btnLanjut.setOnClickListener {
            val tanggal = binding.etTanggal.text.toString()
            val desa = binding.acDesa.text.toString()
            val auditor = binding.etAuditor.text.toString()
            val petani = binding.etPetani.text.toString()

            if (tanggal.isEmpty() || desa.isEmpty() || auditor.isEmpty() || petani.isEmpty()) {
                Toast.makeText(requireContext(), "Semua data wajib diisi!", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch {
                    
                    val currentPeriod = viewModel.auditHeader.periode
                    val lastAudit = database.auditDao().getLastAuditForPetani(petani, currentPeriod)
                    
                    var isFollowUp = viewModel.auditHeader.auditAttempt > 1
                    if (isFollowUp && lastAudit != null) {
                        val answers = database.auditDao().getAnswersForAudit(lastAudit.idAudit)
                        
                        viewModel.auditHeader = viewModel.auditHeader.copy(
                            parentAuditId = lastAudit.idAudit,
                            tanggal = tanggal,
                            desa = desa
                        )
                        
                        // Load all answers to UI/ViewModel, so that true answers are carried over
                        answers.forEach { ans ->
                            viewModel.auditAnswers[ans.questionKey] = ans.answer
                        }
                    } else {
                        viewModel.auditHeader = viewModel.auditHeader.copy(
                            tanggal = tanggal,
                            desa = desa
                        )
                    }

                    (requireActivity() as AuditInternalActivity)
                        .navigateTo(Section2Fragment(),25)
                }
            }

        }
    }
    
    private fun getCurrentPeriod(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // 1-12
        val semester = if (month <= 6) "S1" else "S2"
        return "$year-$semester"
    }

    private fun siapkanDanTampilkanDataMaster() {
        lifecycleScope.launch(Dispatchers.IO) {

            // Ambil data dari Room
            val listDesa = database.masterDao()
                .getAllDesa()
                .map { it.namaDesa }

            withContext(Dispatchers.Main) {

                binding.acDesa.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        listDesa
                    )
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}