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

class KLSection1Fragment : Fragment() {

    private var _binding: FragmentKlSection1Binding? = null
    private val binding get() = _binding!!
    private lateinit var database: AppDatabase
    private val viewModel: KunjunganLahanViewModel by activityViewModels()

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
        binding.etDesaKepengurusan.setText(viewModel.kunjunganLahanForm.desaKepengurusan)
        binding.acAuditor.setText(viewModel.kunjunganLahanForm.namaAuditor, false)
        binding.acPetani.setText(viewModel.kunjunganLahanForm.namaPetani, false)

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
            val listPetani = database.masterDao().getAllPetani().map { it.namaPetani }

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
