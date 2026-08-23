package com.example.notasawit.Admin.KunjunganLahan

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.FragmentKlSection1Binding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.notasawit.Room.Petani.PetaniEntity

class KLSection1Fragment : Fragment() {

    private var _binding: FragmentKlSection1Binding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private val viewModel: KunjunganLahanViewModel by activityViewModels()
    private var listPetaniEntity: List<PetaniEntity> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKlSection1Binding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = requireActivity() as KunjunganLahanActivity
        database = activity.database

        siapkanDanTampilkanDataMaster()
        
        binding.etTanggal.setText(viewModel.kunjunganLahanForm.tanggal)

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
        binding.etDesaKebun.setText(viewModel.kunjunganLahanForm.desaKebun)
        binding.etAuditor.setText(viewModel.kunjunganLahanForm.namaAuditor)
        binding.etPetani.setText(viewModel.kunjunganLahanForm.namaPetani)

        val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", android.content.Context.MODE_PRIVATE)
        val adminDesaId = sharedPref.getInt("admin_desa_id", 0)

        lifecycleScope.launch(Dispatchers.IO) {
            val allDesa = database.masterDao().getAllDesa()
            val desaFromId = allDesa.find { it.idDesa == adminDesaId }?.namaDesa ?: ""
            
            var resolvedDesa = viewModel.kunjunganLahanForm.desaKepengurusan
            if (resolvedDesa.isEmpty() || resolvedDesa == "-") {
                resolvedDesa = desaFromId
            }
            if (resolvedDesa.isEmpty() || resolvedDesa == "-") {
                val selectedPetani = database.masterDao().getAllPetani().find { it.namaPetani == viewModel.kunjunganLahanForm.namaPetani }
                if (selectedPetani != null && selectedPetani.namaDesa.isNotEmpty() && selectedPetani.namaDesa != "-") {
                    resolvedDesa = selectedPetani.namaDesa
                }
            }

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    binding.etDesaKepengurusan.setText(resolvedDesa)
                    viewModel.kunjunganLahanForm = viewModel.kunjunganLahanForm.copy(desaKepengurusan = resolvedDesa)
                }
            }
        }

        binding.btnLanjut.setOnClickListener {
            val tanggal = binding.etTanggal.text.toString()
            val desaKebun = binding.etDesaKebun.text.toString()
            val desaKepengurusan = binding.etDesaKepengurusan.text.toString()
            val auditor = binding.etAuditor.text.toString()
            val petani = binding.etPetani.text.toString()

            if (tanggal.isEmpty() || desaKebun.isEmpty() || desaKepengurusan.isEmpty() || auditor.isEmpty() || petani.isEmpty()) {
                Toast.makeText(requireContext(), "Semua data wajib diisi/dipilih!", Toast.LENGTH_SHORT).show()
            } else {
                lifecycleScope.launch(Dispatchers.IO) {
                    val idPetani = viewModel.kunjunganLahanForm.idPetani

                    val currentPeriod = if (viewModel.kunjunganLahanForm.periode.isNotEmpty()) viewModel.kunjunganLahanForm.periode else {
                        val year = if (tanggal.length >= 4) tanggal.substring(0, 4) else Calendar.getInstance().get(Calendar.YEAR).toString()
                        val month = if (tanggal.length >= 7) (tanggal.substring(5, 7).toIntOrNull() ?: 1) else 1
                        if (month <= 6) "$year-S1" else "$year-S2"
                    }

                    val lastKunjungan = database.KunjunganLahanDao().getLastKunjunganForPetani(petani, currentPeriod)
                    val isFollowUp = viewModel.kunjunganLahanForm.visitAttempt > 1

                    val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", android.content.Context.MODE_PRIVATE)
                    val userId = sharedPref.getInt("user_id", sharedPref.getInt("admin_id", 0))

                    viewModel.kunjunganLahanForm = viewModel.kunjunganLahanForm.copy(
                        tanggal = tanggal,
                        desaKebun = desaKebun,
                        desaKepengurusan = desaKepengurusan,
                        namaAuditor = auditor,
                        namaPetani = petani,
                        idPetani = idPetani,
                        userId = if (userId != 0) userId else viewModel.kunjunganLahanForm.userId,
                        periode = currentPeriod,
                        parentKunjunganId = if (isFollowUp && lastKunjungan != null) lastKunjungan.idKunjungan else null
                    )

                    withContext(Dispatchers.Main) {
                        (requireActivity() as KunjunganLahanActivity).navigateTo(KLSection2Fragment(), 2)
                    }
                }
            }
        }
    }

    private fun siapkanDanTampilkanDataMaster() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", android.content.Context.MODE_PRIVATE)
            val adminDesaId = sharedPref.getInt("admin_desa_id", 0)
            
            listPetaniEntity = if (adminDesaId != 0) {
                database.masterDao().getPetaniByDesa(adminDesaId)
            } else {
                database.masterDao().getAllPetani()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
