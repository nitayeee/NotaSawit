package com.example.notasawit.Admin.AuditInternal

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Admin.AuditInternal.AuditViewModel.AuditViewModel
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.FragmentDashboardAuditBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DashboardAuditFragment : Fragment() {

    private var _binding: FragmentDashboardAuditBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: AuditViewModel
    private lateinit var database: AppDatabase
    private lateinit var adapter: PetaniAuditAdapter

    private var selectedPeriode = ""
    private var selectedStatus = "Semua"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardAuditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(requireActivity())[AuditViewModel::class.java]

        setupDropdowns()
        setupRecyclerView()
        loadData()
    }

    private fun setupDropdowns() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val periodeList = listOf("$currentYear-S1", "$currentYear-S2", "${currentYear-1}-S1", "${currentYear-1}-S2")
        val statusList = listOf("Semua", "Belum Audit", "Lulus", "Perlu Perbaikan")

        val periodeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, periodeList)
        binding.acPeriode.setAdapter(periodeAdapter)
        
        // Auto select current period
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) // 0-11
        selectedPeriode = if (currentMonth < 6) "$currentYear-S1" else "$currentYear-S2"
        binding.acPeriode.setText(selectedPeriode, false)

        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statusList)
        binding.acStatus.setAdapter(statusAdapter)
        binding.acStatus.setText(selectedStatus, false)

        binding.acPeriode.setOnItemClickListener { _, _, position, _ ->
            selectedPeriode = periodeList[position]
            loadData()
        }

        binding.acStatus.setOnItemClickListener { _, _, position, _ ->
            selectedStatus = statusList[position]
            loadData()
        }
    }

    private fun setupRecyclerView() {
        adapter = PetaniAuditAdapter(mutableListOf()) { data ->
            // On Audit Clicked
            val isFollowUp = data.statusAudit == "Perlu Perbaikan"
            val nextAttempt = if (isFollowUp) data.auditAttempt + 1 else 1

            // Ambil nama auditor dari SharedPreferences
            val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
            val username = sharedPref.getString("username", "Admin") ?: "Admin"

            // Update ViewModel state
            viewModel.updatePetaniAndAuditor(data.idPetani, data.namaPetani, username)
            viewModel.updatePeriodeAndAttempt(selectedPeriode, nextAttempt)

            (activity as? AuditInternalActivity)?.navigateTo(Section1Fragment(), 25)
        }
        binding.rvPetaniAudit.adapter = adapter
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val petaniList = database.masterDao().getAllPetani()
            val auditDataList = mutableListOf<PetaniAuditData>()

            for (petani in petaniList) {
                val lastAudit = database.auditDao().getLastAuditForPetani(petani.namaPetani, selectedPeriode)
                
                val status = lastAudit?.statusAudit ?: "Belum Audit"
                val tgl = lastAudit?.tanggal ?: "-"
                val pdf = lastAudit?.pdfPath ?: ""
                val attempt = lastAudit?.auditAttempt ?: 0

                auditDataList.add(
                    PetaniAuditData(
                        idPetani = petani.idPetani,
                        namaPetani = petani.namaPetani,
                        desa = petani.namaDesa ?: "-",
                        statusAudit = status,
                        tanggalAudit = tgl,
                        pdfPath = pdf,
                        auditAttempt = attempt
                    )
                )
            }

            // Apply filter
            val filteredList = if (selectedStatus == "Semua") {
                auditDataList
            } else {
                auditDataList.filter { it.statusAudit == selectedStatus }
            }

            withContext(Dispatchers.Main) {
                adapter.updateData(filteredList)
            }
        }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
