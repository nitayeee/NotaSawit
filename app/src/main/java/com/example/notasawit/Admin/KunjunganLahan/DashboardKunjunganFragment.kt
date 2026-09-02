package com.example.notasawit.Admin.KunjunganLahan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.KunjunganLahanEntity.KunjunganLahanForm
import com.example.notasawit.databinding.FragmentDashboardAuditBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class DashboardKunjunganFragment : Fragment() {

    private var _binding: FragmentDashboardAuditBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: KunjunganLahanViewModel
    private lateinit var database: AppDatabase
    private lateinit var adapter: PetaniKunjunganAdapter

    private var selectedPeriode = ""
    private var selectedStatus = "Semua"
    private var searchQuery = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardAuditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        database = AppDatabase.getDatabase(requireContext())
        viewModel = ViewModelProvider(requireActivity())[KunjunganLahanViewModel::class.java]

        setupDropdowns()
        setupRecyclerView()

        binding.etSearchPetani.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s?.toString()?.trim() ?: ""
                loadData()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.swipeRefresh.setOnRefreshListener {
            fetchLahanFromServer { fetchKunjunganFromServer() }
        }

        loadData()
        fetchLahanFromServer { fetchKunjunganFromServer() }
    }

    private fun fetchLahanFromServer(onComplete: () -> Unit = {}) {
        com.example.notasawit.Network.PetaniApi.getAllLahan(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                lifecycleScope.launch(Dispatchers.Main) {
                    onComplete()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        try {
                            val jsonObject = org.json.JSONObject(body)
                            val dataArray = jsonObject.getJSONArray("data")
                            val lahanList = mutableListOf<com.example.notasawit.Room.Lahan.LahanEntity>()

                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val lahanId = item.optInt("lahan_id", item.optInt("id", 0))
                                if (lahanId == 0) continue

                                lahanList.add(
                                    com.example.notasawit.Room.Lahan.LahanEntity(
                                        lahan_id = lahanId,
                                        petani_id = item.optInt("petani_id", item.optInt("id_petani", 0)),
                                        lahan_nama = item.optString("lahan_nama", item.optString("nama_lahan", "")),
                                        lahan_luas = item.optDouble("lahan_luas", item.optDouble("luas_lahan", 0.0))
                                    )
                                )
                            }
                            if (lahanList.isNotEmpty()) {
                                database.LahanDao().insertLahan(lahanList)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            withContext(Dispatchers.Main) {
                                onComplete()
                            }
                        }
                    }
                } else {
                    lifecycleScope.launch(Dispatchers.Main) {
                        onComplete()
                    }
                }
            }
        })
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

    private fun fetchKunjunganFromServer() {
        if (_binding == null) return
        binding.swipeRefresh.isRefreshing = true
        val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val adminDesaNama = sharedPref.getString("admin_desa", "")

        com.example.notasawit.Network.PetaniApi.getAllKunjunganByDesa(if (adminDesaNama.isNullOrEmpty()) null else adminDesaNama, object : okhttp3.Callback {
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
                                val idKunjungan = item.optString("id_kunjungan", item.optString("id", ""))
                                if (idKunjungan.isEmpty()) continue

                                val status = if (item.isNull("status_kunjungan")) "Belum Kunjungan" else item.optString("status_kunjungan", "Belum Kunjungan")
                                val keterangan = if (item.isNull("keterangan")) "" else item.optString("keterangan", "")
                                val tanggal = item.optString("tanggal_kunjungan", item.optString("tanggal", ""))
                                val desaKebun = item.optString("desa_kebun", "")
                                val desaKepengurusan = item.optString("desa_kepengurusan", "")

                                var userId = item.optInt("user_id", item.optInt("id_user", 0))
                                var namaAuditor = item.optString("nama_auditor", item.optString("auditor_nama", ""))
                                if (item.has("user") && !item.isNull("user")) {
                                    val uObj = item.optJSONObject("user")
                                    if (namaAuditor.isEmpty()) {
                                        namaAuditor = uObj?.optString("user_nama", uObj.optString("nama_user", "")) ?: ""
                                    }
                                    if (userId == 0) {
                                        userId = uObj?.optInt("user_id", uObj.optInt("id", 0)) ?: 0
                                    }
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

                                val visitAttempt = item.optInt("visit_attempt", item.optInt("attempt", 1))
                                var pdfPath = ""
                                val itemKeys = item.keys()
                                while (itemKeys.hasNext()) {
                                    val k = itemKeys.next()
                                    val v = item.optString(k, "").trim()
                                    if (v.contains(".pdf", ignoreCase = true) && v != "null") {
                                        pdfPath = v
                                        break
                                    }
                                }
                                if (pdfPath.isEmpty()) {
                                    val possibleKeys = listOf(
                                        "file_kunjungan", "file_pdf", "pdf_path", "path_pdf", "file_audit",
                                        "file", "pdf", "url_pdf", "file_url", "pdf_file", "link_pdf", "url", "path", "file_name"
                                    )
                                    for (key in possibleKeys) {
                                        if (item.has(key) && !item.isNull(key)) {
                                            val valStr = item.optString(key, "").trim()
                                            if (valStr.isNotEmpty() && valStr != "null") {
                                                pdfPath = valStr
                                                break
                                            }
                                        }
                                    }
                                }
                                val periode = item.optString("periode", "")

                                val calculatedPeriode = parsePeriode(tanggal, periode)
                                val existing = database.KunjunganLahanDao().getKunjunganLahanById(idKunjungan)
                                val resolvedPdfPath: String = if (pdfPath.isNotEmpty()) pdfPath else (existing?.pdfPath ?: "")

                                val kunjungan = KunjunganLahanForm(
                                    idKunjungan = idKunjungan,
                                    tanggal = if (tanggal.isNotEmpty()) tanggal else (existing?.tanggal ?: ""),
                                    desaKebun = if (desaKebun.isNotEmpty()) desaKebun else (existing?.desaKebun ?: ""),
                                    desaKepengurusan = if (desaKepengurusan.isNotEmpty()) desaKepengurusan else (existing?.desaKepengurusan ?: ""),
                                    namaAuditor = if (namaAuditor.isNotEmpty()) namaAuditor else (existing?.namaAuditor ?: "Auditor"),
                                    namaPetani = if (namaPetani.isNotEmpty()) namaPetani else (existing?.namaPetani ?: ""),
                                    idPetani = if (petaniId != 0) petaniId else existing?.idPetani,
                                    userId = if (userId != 0) userId else existing?.userId,
                                    ringkasanTemuan = keterangan,
                                    statusKunjungan = status,
                                    periode = calculatedPeriode,
                                    visitAttempt = visitAttempt,
                                    pdfPath = resolvedPdfPath,
                                    isSynced = true
                                )
                                database.KunjunganLahanDao().insertKunjunganLahan(kunjungan)
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
        val statusList = listOf("Semua", "Belum Kunjungan", "Selesai", "Perlu Perbaikan")

        val periodeAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, periodeList)
        binding.acPeriode.setAdapter(periodeAdapter)

        val currentMonth = Calendar.getInstance().get(Calendar.MONTH)
        selectedPeriode = if (currentMonth < 6) "$currentYear-S1" else "$currentYear-S2"
        binding.acPeriode.setText(selectedPeriode, false)

        val statusAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, statusList)
        binding.acStatus.setAdapter(statusAdapter)
        binding.acStatus.setText(selectedStatus, false)

        binding.acPeriode.setOnClickListener {
            binding.acPeriode.showDropDown()
        }

        binding.acStatus.setOnClickListener {
            binding.acStatus.showDropDown()
        }

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
        adapter = PetaniKunjunganAdapter(mutableListOf()) { data, lahanItem ->
            val isFollowUp = lahanItem.statusLahan == "Perlu Perbaikan"
            val nextAttempt = if (isFollowUp) lahanItem.latestAttempt + 1 else 1

            val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
            val username = sharedPref.getString("username", "Admin") ?: "Admin"
            val userId = sharedPref.getInt("user_id", sharedPref.getInt("admin_id", 0))

            val todayStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            viewModel.resetForm()
            viewModel.updatePetaniAndAuditor(data.idPetani, data.namaPetani, username, userId)
            viewModel.updatePeriodeAndAttempt(selectedPeriode, nextAttempt)
            val adminDesaNama = sharedPref.getString("admin_desa", "") ?: ""
            viewModel.kunjunganLahanForm = viewModel.kunjunganLahanForm.copy(
                tanggal = todayStr,
                desaKebun = lahanItem.namaLahan,
                desaKepengurusan = if (data.desa.isNotEmpty() && data.desa != "-") data.desa else adminDesaNama
            )

            val previousIdKunjungan = if (isFollowUp && lahanItem.history.isNotEmpty()) {
                val latest = lahanItem.history.maxByOrNull { it.visitAttempt } ?: lahanItem.history.first()
                latest.idKunjungan
            } else null

            lifecycleScope.launch(Dispatchers.IO) {
                if (previousIdKunjungan != null) {
                    val prevForm = database.KunjunganLahanDao().getKunjunganLahanById(previousIdKunjungan)
                    if (prevForm != null) {
                        viewModel.previousAnswers["q1_patokBatas"] = prevForm.q1_patokBatas ?: false
                        viewModel.previousAnswers["q2_idKebun"] = prevForm.q2_idKebun ?: false
                        viewModel.previousAnswers["q3_piringanPasarPikul"] = prevForm.q3_piringanPasarPikul ?: false
                        viewModel.previousAnswers["q4_pelepahDitunas"] = prevForm.q4_pelepahDitunas ?: false
                        viewModel.previousAnswers["q5_susunanPelepah"] = prevForm.q5_susunanPelepah ?: false
                        viewModel.previousAnswers["q6_turnera"] = prevForm.q6_turnera ?: false
                        viewModel.previousAnswers["q7_bekasPembakaran"] = prevForm.q7_bekasPembakaran ?: false
                        viewModel.previousAnswers["q8_botolRacunPlastik"] = prevForm.q8_botolRacunPlastik ?: false
                        viewModel.previousAnswers["q9_sampahPlastik"] = prevForm.q9_sampahPlastik ?: false
                        viewModel.previousAnswers["q10_plangSungai"] = prevForm.q10_plangSungai ?: false
                        viewModel.previousAnswers["q11_semprotSungai"] = prevForm.q11_semprotSungai ?: false
                        viewModel.previousAnswers["q12_sampahSungai"] = prevForm.q12_sampahSungai ?: false
                        viewModel.previousAnswers["q13_semprotTotal"] = prevForm.q13_semprotTotal ?: false
                        viewModel.previousAnswers["q14_racunKontak"] = prevForm.q14_racunKontak ?: false
                        viewModel.previousAnswers["q15_hamaPenyakit"] = prevForm.q15_hamaPenyakit ?: false
                    }
                }

                viewModel.previousAnswers.forEach { (key, value) ->
                    if (value) {
                        viewModel.kunjunganAnswers[key] = true
                    }
                }

                withContext(Dispatchers.Main) {
                    (requireActivity() as KunjunganLahanActivity).navigateTo(KLSection1Fragment(), 1)
                }
            }
        }

        binding.rvPetaniAudit.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPetaniAudit.adapter = adapter
    }

    private fun loadData() {
        if (_binding == null) return
        val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)

        lifecycleScope.launch(Dispatchers.IO) {
            val adminDesaId = sharedPref.getInt("admin_desa_id", 0)

            val petaniList = if (adminDesaId != 0) {
                database.masterDao().getPetaniByDesa(adminDesaId)
            } else {
                database.masterDao().getAllPetani()
            }
            val kunjunganDataList = mutableListOf<PetaniKunjunganData>()
            val allLahanList = database.LahanDao().getAllLahan()

            for (petani in petaniList) {
                var allKunjungan = database.KunjunganLahanDao().getAllKunjunganForPetani(petani.idPetani, petani.namaPetani, selectedPeriode)
                if (allKunjungan.isEmpty()) {
                    val anyKunjungan = database.KunjunganLahanDao().getAllKunjunganForPetaniAllPeriods(petani.idPetani, petani.namaPetani)
                    if (anyKunjungan.isNotEmpty()) {
                        allKunjungan = anyKunjungan
                    }
                }

                val petaniLahanList = allLahanList.filter { it.petani_id == petani.idPetani }
                val lahanKunjunganItems = mutableListOf<LahanKunjunganItem>()

                if (petaniLahanList.isNotEmpty()) {
                    for (lahan in petaniLahanList) {
                        val matched = allKunjungan.filter {
                            it.desaKebun.equals(lahan.lahan_nama, ignoreCase = true) ||
                            it.desaKebun.contains(lahan.lahan_nama, ignoreCase = true) ||
                            lahan.lahan_nama.contains(it.desaKebun, ignoreCase = true)
                        }.sortedByDescending { it.visitAttempt }

                        val status = if (matched.isEmpty()) "Belum Kunjungan" else (matched.first().statusKunjungan ?: "Belum Kunjungan")
                        val historyList = matched.map { k ->
                            KunjunganHistoryItem(
                                idKunjungan = k.idKunjungan,
                                tanggal = k.tanggal ?: "",
                                namaAuditor = k.namaAuditor ?: "",
                                statusKunjungan = k.statusKunjungan ?: "",
                                pdfPath = k.pdfPath ?: "",
                                visitAttempt = k.visitAttempt
                            )
                        }

                        lahanKunjunganItems.add(
                            LahanKunjunganItem(
                                lahanId = lahan.lahan_id,
                                namaLahan = lahan.lahan_nama,
                                luasLahan = lahan.lahan_luas,
                                statusLahan = status,
                                latestAttempt = matched.firstOrNull()?.visitAttempt ?: 0,
                                history = historyList
                            )
                        )
                    }
                } else if (allKunjungan.isNotEmpty()) {
                    val historyList = allKunjungan.sortedByDescending { it.visitAttempt }.map { k ->
                        KunjunganHistoryItem(
                            idKunjungan = k.idKunjungan,
                            tanggal = k.tanggal ?: "",
                            namaAuditor = k.namaAuditor ?: "",
                            statusKunjungan = k.statusKunjungan ?: "",
                            pdfPath = k.pdfPath ?: "",
                            visitAttempt = k.visitAttempt
                        )
                    }
                    val latest = allKunjungan.maxByOrNull { it.visitAttempt }
                    val landName = latest?.desaKebun.takeIf { !it.isNullOrEmpty() } ?: "Lahan Utama"
                    val status = latest?.statusKunjungan ?: "Belum Kunjungan"

                    lahanKunjunganItems.add(
                        LahanKunjunganItem(
                            lahanId = 0,
                            namaLahan = landName,
                            luasLahan = 0.0,
                            statusLahan = status,
                            latestAttempt = latest?.visitAttempt ?: 0,
                            history = historyList
                        )
                    )
                }

                val overallStatus = when {
                    lahanKunjunganItems.isEmpty() -> "Belum Kunjungan"
                    lahanKunjunganItems.any { it.statusLahan == "Perlu Perbaikan" } -> "Perlu Perbaikan"
                    lahanKunjunganItems.all { it.statusLahan == "Lulus" || it.statusLahan == "Selesai" } -> "Lulus"
                    lahanKunjunganItems.any { it.statusLahan == "Lulus" || it.statusLahan == "Selesai" } -> "Lulus Sebagian"
                    else -> "Belum Kunjungan"
                }

                kunjunganDataList.add(
                    PetaniKunjunganData(
                        idPetani = petani.idPetani,
                        namaPetani = petani.namaPetani,
                        desa = petani.namaDesa ?: "-",
                        statusKunjungan = overallStatus,
                        fotoProfil = petani.fotoProfil,
                        lahanList = lahanKunjunganItems
                    )
                )
            }

            var filteredSequence = kunjunganDataList.asSequence()
            if (selectedStatus != "Semua") {
                filteredSequence = filteredSequence.filter { it.statusKunjungan == selectedStatus }
            }
            if (searchQuery.isNotEmpty()) {
                filteredSequence = filteredSequence.filter { it.namaPetani.contains(searchQuery, ignoreCase = true) }
            }
            val filteredList = filteredSequence.toList()

            withContext(Dispatchers.Main) {
                if (_binding != null) {
                    if (filteredList.isEmpty()) {
                        binding.tvEmptyPetani.visibility = View.VISIBLE
                        binding.rvPetaniAudit.visibility = View.GONE
                    } else {
                        binding.tvEmptyPetani.visibility = View.GONE
                        binding.rvPetaniAudit.visibility = View.VISIBLE
                    }
                    adapter.updateData(filteredList)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
