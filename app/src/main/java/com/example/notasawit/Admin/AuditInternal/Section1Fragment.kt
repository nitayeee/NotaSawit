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
import com.example.notasawit.Room.Desa.DesaEntity
import com.example.notasawit.Room.Petani.PetaniEntity
import com.example.notasawit.databinding.FragmentSection1Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.fragment.app.activityViewModels
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
            // A. CEK JIKA ROOM MASIH KOSONG, ISI DATA DUMMY (Palsu)
            val cekPetani = database.masterDao().getAllPetani()
            if (cekPetani.isEmpty()) {
                val dummyDesa = listOf(
                    DesaEntity(1, "Desa Sawit Makmur"),
                    DesaEntity(2, "Desa Riau Sejahtera"),
                    DesaEntity(3, "Desa Tunas Muda")
                )
                val dummyAuditor = listOf(
                    AuditorEntity(1, "Budi Santoso", "budi123"),
                    AuditorEntity(2, "Siti Aminah", "siti456")
                )
                val dummyPetani = listOf(
                    PetaniEntity(1, "Ahmad Subarjo", "Desa Sawit Makmur"),
                    PetaniEntity(2, "Anwar Ibrahim", "Desa Riau Sejahtera"),
                    PetaniEntity(3, "Amiruddin", "Desa Tunas Muda"),
                    PetaniEntity(4, "Bambang Pamungkas", "Desa Sawit Makmur")
                )

                // Simpan dummies ke Room
                database.masterDao().insertDesa(dummyDesa)
                database.masterDao().insertAuditor(dummyAuditor)
                database.masterDao().insertPetani(dummyPetani)
            }

            // B. AMBIL DATA DARI ROOM UNTUK DIOPER KE UI
            val listDesa = database.masterDao().getAllDesa().map { it.namaDesa }
            val listAuditor = database.masterDao().getAllAuditor().map { it.namaAuditor }
            val listPetani = database.masterDao().getAllPetani().map { it.namaPetani }

            // C. PASANG KE SEARCHABLE DROPDOWN (Pindah ke Main Thread)
            withContext(Dispatchers.Main) {
                // Adapter Desa
                val adapterDesa = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    listDesa
                )
                binding.acDesa.setAdapter(adapterDesa)

                // Adapter Auditor
                val adapterAuditor = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listAuditor)
                binding.acAuditor.setAdapter(adapterAuditor)

                // Adapter Petani
                val adapterPetani = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, listPetani)
                binding.acPetani.setAdapter(adapterPetani)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}