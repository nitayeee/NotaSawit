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
        binding.etDesaKebun.setText(viewModel.kunjunganLahanForm.desaKebun, false)
        binding.etDesaKepengurusan.setText(viewModel.kunjunganLahanForm.desaKepengurusan, false)
        binding.acAuditor.setText(viewModel.kunjunganLahanForm.namaAuditor, false)
        binding.acPetani.setText(viewModel.kunjunganLahanForm.namaPetani, false)

        binding.acPetani.setOnItemClickListener { _, _, position, _ ->
            val selectedPetaniName = binding.acPetani.adapter.getItem(position) as String
            val selectedPetani = listPetaniEntity.find { it.namaPetani == selectedPetaniName }
            if (selectedPetani != null) {
                updateLahanDropdown(selectedPetani.idPetani)
            }
        }

        binding.btnLanjut.setOnClickListener {
            val tanggal = binding.etTanggal.text.toString()
            val desaKebun = binding.etDesaKebun.text.toString()
            val desaKepengurusan = binding.etDesaKepengurusan.text.toString()
            val auditor = binding.acAuditor.text.toString()
            val petani = binding.acPetani.text.toString()

            if (tanggal.isEmpty() || desaKebun.isEmpty() || desaKepengurusan.isEmpty() || auditor.isEmpty() || petani.isEmpty()) {
                Toast.makeText(requireContext(), "Semua data wajib diisi/dipilih!", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.kunjunganLahanForm = viewModel.kunjunganLahanForm.copy(
                    tanggal = tanggal,
                    desaKebun = desaKebun,
                    desaKepengurusan = desaKepengurusan,
                    namaAuditor = auditor,
                    namaPetani = petani
                )

                (requireActivity() as KunjunganLahanActivity).navigateTo(KLSection2Fragment(), 2)
            }
        }
    }

    private fun siapkanDanTampilkanDataMaster() {
        lifecycleScope.launch(Dispatchers.IO) {
            val listAuditor = database.masterDao().getAllAuditor().map { it.namaAuditor }
            
            val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", android.content.Context.MODE_PRIVATE)
            val adminDesaId = sharedPref.getInt("admin_desa_id", 0)
            
            listPetaniEntity = if (adminDesaId != 0) {
                database.masterDao().getPetaniByDesa(adminDesaId)
            } else {
                database.masterDao().getAllPetani()
            }
            val listPetaniNames = listPetaniEntity.map { it.namaPetani }
            val listDesaNames = database.masterDao().getAllDesa().map { it.namaDesa }

            withContext(Dispatchers.Main) {
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
                        listPetaniNames
                    )
                )

                binding.etDesaKepengurusan.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        listDesaNames
                    )
                )

                // Jika sudah ada petani yang dipilih sebelumnya, load lahan-lahannya
                val savedPetaniName = viewModel.kunjunganLahanForm.namaPetani
                if (savedPetaniName.isNotEmpty()) {
                    val selectedPetani = listPetaniEntity.find { it.namaPetani == savedPetaniName }
                    if (selectedPetani != null) {
                        updateLahanDropdown(selectedPetani.idPetani, clearText = false)
                    }
                }
            }
        }
    }

    private fun updateLahanDropdown(petaniId: Int, clearText: Boolean = true) {
        com.example.notasawit.Network.PetaniApi.getLahanByPetani(petaniId, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                loadLahanDariRoom(petaniId, clearText)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (json != null) {
                        try {
                            val lahanResponse = com.google.gson.Gson().fromJson(json, com.example.notasawit.Model.LahanResponse::class.java)
                            val lahanEntity = lahanResponse.data.map {
                                com.example.notasawit.Room.Lahan.LahanEntity(
                                    lahan_id = it.lahan_id,
                                    petani_id = petaniId,
                                    lahan_nama = it.lahan_nama,
                                    lahan_luas = it.lahan_luas
                                )
                            }
                            
                            lifecycleScope.launch(Dispatchers.IO) {
                                database.LahanDao().insertLahan(lahanEntity)
                                loadLahanDariRoom(petaniId, clearText)
                            }
                        } catch (e: Exception) {
                            loadLahanDariRoom(petaniId, clearText)
                        }
                    } else {
                        loadLahanDariRoom(petaniId, clearText)
                    }
                } else {
                    loadLahanDariRoom(petaniId, clearText)
                }
            }
        })
    }

    private fun loadLahanDariRoom(petaniId: Int, clearText: Boolean) {
        lifecycleScope.launch(Dispatchers.IO) {
            val lahanList = database.LahanDao().getAllLahan().filter { it.petani_id == petaniId }
            val lahanNames = lahanList.map { it.lahan_nama }

            withContext(Dispatchers.Main) {
                if (lahanNames.isEmpty()) {
                    Toast.makeText(requireContext(), "Tidak ada lahan untuk petani ini", Toast.LENGTH_SHORT).show()
                }

                binding.etDesaKebun.setAdapter(
                    ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        lahanNames
                    )
                )
                if (clearText) {
                    binding.etDesaKebun.setText("", false)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
