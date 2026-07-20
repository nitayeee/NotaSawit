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
        binding.etTanggal.setText(viewModel.auditForm.tanggal)

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
        binding.acDesa.setText(viewModel.auditForm.desa, false)
        binding.acAuditor.setText(viewModel.auditForm.namaAuditor, false)
        binding.acPetani.setText(viewModel.auditForm.namaPetani, false)

        // 3. Logika tombol Lanjut ke Section 2
        binding.btnLanjut.setOnClickListener {
            val tanggal = binding.etTanggal.text.toString()
            val desa = binding.acDesa.text.toString()
            val auditor = binding.acAuditor.text.toString()
            val petani = binding.acPetani.text.toString()

            if (tanggal.isEmpty() || desa.isEmpty() || auditor.isEmpty() || petani.isEmpty()) {
                Toast.makeText(requireContext(), "Semua data wajib diisi/dipilih!", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.auditForm = viewModel.auditForm.copy(

                    tanggal = binding.etTanggal.text.toString(),

                    desa = binding.acDesa.text.toString(),

                    namaAuditor = binding.acAuditor.text.toString(),

                    namaPetani = binding.acPetani.text.toString()

                )

                (requireActivity() as AuditInternalActivity)
                    .navigateTo(Section2Fragment(),2)
            }

        }
    }

    private fun siapkanDanTampilkanDataMaster() {
        lifecycleScope.launch(Dispatchers.IO) {



            // Ambil data dari Room
            val listDesa = database.masterDao()
                .getAllDesa()
                .map { it.namaDesa }

            val listAuditor = database.masterDao()
                .getAllAuditor()
                .map { it.namaAuditor }

            val listPetani = database.masterDao()
                .getAllPetani()
                .map { it.namaPetani }

            withContext(Dispatchers.Main) {

                binding.acDesa.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        listDesa
                    )
                )

                binding.acAuditor.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        listAuditor
                    )
                )

                binding.acPetani.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        listPetani
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