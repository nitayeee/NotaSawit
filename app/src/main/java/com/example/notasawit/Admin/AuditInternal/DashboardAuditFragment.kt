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
        
        binding.swipeRefresh.setOnRefreshListener {
            fetchAuditsFromServer()
        }
        
        loadData()
    }
    
    private fun fetchAuditsFromServer() {
        binding.swipeRefresh.isRefreshing = true
        val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val adminDesaNama = sharedPref.getString("admin_desa", "") // Ensure admin_desa is saved in Login or use null to get all

        com.example.notasawit.Network.PetaniApi.getAllAuditByDesa(if (adminDesaNama.isNullOrEmpty()) null else adminDesaNama, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                lifecycleScope.launch(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = false
                    android.widget.Toast.makeText(requireContext(), "Gagal terhubung ke server", android.widget.Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                val jsonObject = org.json.JSONObject(body)
                                val dataArray = jsonObject.getJSONArray("data")

                                for (i in 0 until dataArray.length()) {
                                    val item = dataArray.getJSONObject(i)
                                    val idAudit = item.getString("id_audit")
                                    
                                    if (item.isNull("status_audit")) continue
                                    val status = item.getString("status_audit")
                                    
                                    // Mencegah NOT NULL constraint failed dengan pass "" jika null
                                    val keterangan = if (item.isNull("keterangan")) "" else item.getString("keterangan")
                                    
                                    database.auditDao().updateAuditStatus(idAudit, status, keterangan)
                                }
                                
                                withContext(Dispatchers.Main) {
                                    binding.swipeRefresh.isRefreshing = false
                                    loadData()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    binding.swipeRefresh.isRefreshing = false
                                }
                            }
                        }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        binding.swipeRefresh.isRefreshing = false
                    }
                }
            }
        })
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

            val previousIdAudit = if (isFollowUp && data.history.isNotEmpty()) {
                val latestAudit = data.history.maxByOrNull { it.auditAttempt } ?: data.history.first()
                latestAudit.idAudit
            } else null

            lifecycleScope.launch(Dispatchers.IO) {
                // Bersihkan previousAnswers dulu
                viewModel.previousAnswers.clear()
                viewModel.auditAnswers.clear() // Bersihkan juga current answers untuk audit baru
                
                if (previousIdAudit != null) {
                    val answers = database.auditDao().getAnswersForAudit(previousIdAudit)
                    answers.forEach {
                        viewModel.previousAnswers[it.questionKey] = it.answer
                    }
                }
                
                // Copy previous true answers to current answers to pre-fill them
                viewModel.previousAnswers.forEach { (key, value) ->
                    if (value == true) {
                        viewModel.auditAnswers[key] = true
                    }
                }

                withContext(Dispatchers.Main) {
                    (activity as? AuditInternalActivity)?.navigateTo(Section1Fragment(), 25)
                }
            }
        }
        binding.rvPetaniAudit.adapter = adapter
    }

    private fun loadData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
            val adminDesaId = sharedPref.getInt("admin_desa_id", 0)
            
            val petaniList = if (adminDesaId != 0) {
                database.masterDao().getPetaniByDesa(adminDesaId)
            } else {
                database.masterDao().getAllPetani()
            }
            val auditDataList = mutableListOf<PetaniAuditData>()

            for (petani in petaniList) {
                val allAudits = database.auditDao().getAllAuditsForPetani(petani.namaPetani, selectedPeriode)
                
                if (allAudits.isEmpty()) {
                    auditDataList.add(
                        PetaniAuditData(
                            idPetani = petani.idPetani,
                            namaPetani = petani.namaPetani,
                            desa = petani.namaDesa ?: "-",
                            statusAudit = "Belum Audit",
                            tanggalAudit = "-",
                            pdfPath = "",
                            auditAttempt = 0,
                            history = emptyList()
                        )
                    )
                } else {
                    // allAudits is sorted by attempt or date (usually we want latest first)
                    // Wait, let's assume allAudits is ordered. AuditDao usually returns latest last, or we can sort it.
                    val latestAudit = allAudits.maxByOrNull { it.auditAttempt } ?: allAudits.first()
                    
                    auditDataList.add(
                        PetaniAuditData(
                            idPetani = petani.idPetani,
                            namaPetani = petani.namaPetani,
                            desa = petani.namaDesa ?: "-",
                            statusAudit = latestAudit.statusAudit ?: "Belum Audit",
                            tanggalAudit = latestAudit.tanggal ?: "-",
                            pdfPath = latestAudit.pdfPath ?: "",
                            auditAttempt = latestAudit.auditAttempt ?: 0,
                            history = allAudits.sortedByDescending { it.auditAttempt }
                        )
                    )
                }
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
