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
        fetchAuditsFromServer()
    }
    
    private fun parsePeriode(tanggal: String, periodeFromApi: String?): String {
        if (!periodeFromApi.isNullOrEmpty() && periodeFromApi.contains("-S")) {
            return periodeFromApi
        }
        if (tanggal.length >= 8) {
            if (tanggal[4] == '-' || tanggal[4] == '/') {
                val year = tanggal.substring(0, 4)
                val month = tanggal.substring(5, 7).toIntOrNull() ?: 1
                return if (month <= 6) "$year-S1" else "$year-S2"
            }
            if (tanggal[2] == '/' || tanggal[2] == '-') {
                val parts = tanggal.split("/", "-")
                if (parts.size >= 3) {
                    val year = parts[2].take(4)
                    val month = parts[1].toIntOrNull() ?: 1
                    return if (month <= 6) "$year-S1" else "$year-S2"
                }
            }
        }
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return "$currentYear-S1"
    }

    private fun fetchAuditsFromServer() {
        if (_binding == null) return
        binding.swipeRefresh.isRefreshing = true
        val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val adminDesaNama = sharedPref.getString("admin_desa", "")

        com.example.notasawit.Network.PetaniApi.getAllAuditByDesa(if (adminDesaNama.isNullOrEmpty()) null else adminDesaNama, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                lifecycleScope.launch(Dispatchers.Main) {
                    _binding?.swipeRefresh?.isRefreshing = false
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
                                    val idAudit = item.optString("id_audit", item.optString("id", ""))
                                    if (idAudit.isEmpty()) continue

                                    val status = if (item.isNull("status_audit")) "Belum Audit" else item.optString("status_audit", "Belum Audit")
                                    val keterangan = if (item.isNull("keterangan")) "" else item.optString("keterangan", "")
                                    val tanggal = item.optString("tanggal", "")
                                    val desa = item.optString("desa", "")

                                    var namaAuditor = item.optString("nama_auditor", item.optString("auditor_nama", ""))
                                    if (namaAuditor.isEmpty() && item.has("user") && !item.isNull("user")) {
                                        namaAuditor = item.optJSONObject("user")?.optString("user_nama", item.optJSONObject("user")?.optString("nama_user", "")) ?: ""
                                    }

                                    var namaPetani = item.optString("nama_petani", item.optString("petani_nama", ""))
                                    var petaniId = item.optInt("petani_id", item.optInt("id_petani", 0))
                                    if (item.has("petani") && !item.isNull("petani")) {
                                        val pObj = item.optJSONObject("petani")
                                        if (namaPetani.isEmpty()) {
                                            namaPetani = pObj?.optString("petani_nama", pObj.optString("nama_petani", "")) ?: ""
                                        }
                                        if (petaniId == 0) {
                                            petaniId = pObj?.optInt("petani_id", pObj.optInt("id_petani", 0)) ?: 0
                                        }
                                    }

                                    val auditAttempt = item.optInt("audit_attempt", item.optInt("attempt", 1))
                                    val pdfPath = item.optString("file_kunjungan", item.optString("pdf_path", ""))
                                    val periode = item.optString("periode", "")

                                    val calculatedPeriode = parsePeriode(tanggal, periode)

                                    val existingAudit = database.auditDao().getAllAuditHeaders().find { it.idAudit == idAudit }

                                    val resolvedPdfPath: String = if (pdfPath.isNotEmpty()) pdfPath else (existingAudit?.pdfPath ?: "")

                                    val header = com.example.notasawit.Room.AuditEntity.AuditHeader(
                                        idAudit = idAudit,
                                        tanggal = if (tanggal.isNotEmpty()) tanggal else (existingAudit?.tanggal ?: ""),
                                        desa = if (desa.isNotEmpty()) desa else (existingAudit?.desa ?: ""),
                                        namaAuditor = if (namaAuditor.isNotEmpty()) namaAuditor else (existingAudit?.namaAuditor ?: "Auditor"),
                                        namaPetani = if (namaPetani.isNotEmpty()) namaPetani else (existingAudit?.namaPetani ?: ""),
                                        idPetani = if (petaniId != 0) petaniId else existingAudit?.idPetani,
                                        ringkasanTemuan = keterangan,
                                        statusAudit = status,
                                        periode = calculatedPeriode,
                                        auditAttempt = auditAttempt,
                                        pdfPath = resolvedPdfPath,
                                        isSynced = true
                                    )
                                    database.auditDao().insertHeader(header)
                                }
                                
                                withContext(Dispatchers.Main) {
                                    _binding?.swipeRefresh?.isRefreshing = false
                                    loadData()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                withContext(Dispatchers.Main) {
                                    _binding?.swipeRefresh?.isRefreshing = false
                                }
                            }
                        }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        _binding?.swipeRefresh?.isRefreshing = false
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
                var allAudits = database.auditDao().getAllAuditsForPetani(petani.idPetani, petani.namaPetani, selectedPeriode)
                val hasAuditsInSelectedPeriode = allAudits.isNotEmpty()
                
                if (allAudits.isEmpty()) {
                    val anyAudits = database.auditDao().getAllAuditsForPetaniAllPeriods(petani.idPetani, petani.namaPetani)
                    if (anyAudits.isNotEmpty()) {
                        allAudits = anyAudits
                    }
                }
                
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
                    val latestAudit = allAudits.maxByOrNull { it.auditAttempt } ?: allAudits.first()
                    val periodLabel = if (!hasAuditsInSelectedPeriode && latestAudit.periode.isNotEmpty()) " (${latestAudit.periode})" else ""
                    
                    auditDataList.add(
                        PetaniAuditData(
                            idPetani = petani.idPetani,
                            namaPetani = petani.namaPetani,
                            desa = petani.namaDesa ?: "-",
                            statusAudit = if (latestAudit.statusAudit.isNullOrEmpty()) "Belum Audit" else latestAudit.statusAudit,
                            tanggalAudit = if (latestAudit.tanggal.isNullOrEmpty()) "-" else latestAudit.tanggal,
                            pdfPath = latestAudit.pdfPath ?: "",
                            auditAttempt = latestAudit.auditAttempt,
                            auditLabel = periodLabel,
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
