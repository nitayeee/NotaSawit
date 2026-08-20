package com.example.notasawit.Admin.KunjunganLahan

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
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

        binding.swipeRefresh.setOnRefreshListener {
            fetchKunjunganFromServer()
        }

        loadData()
        fetchKunjunganFromServer()
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

                                val visitAttempt = item.optInt("visit_attempt", item.optInt("attempt", 1))
                                val pdfPath = item.optString("file_kunjungan", item.optString("pdf_path", ""))
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
        adapter = PetaniKunjunganAdapter(mutableListOf()) { data ->
            val isFollowUp = data.statusKunjungan == "Perlu Perbaikan"
            val nextAttempt = if (isFollowUp) data.visitAttempt + 1 else 1

            val sharedPref = requireContext().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
            val username = sharedPref.getString("username", "Admin") ?: "Admin"

            viewModel.resetForm()
            viewModel.updatePetaniAndAuditor(data.idPetani, data.namaPetani, username)
            viewModel.updatePeriodeAndAttempt(selectedPeriode, nextAttempt)

            val previousIdKunjungan = if (isFollowUp && data.history.isNotEmpty()) {
                val latest = data.history.maxByOrNull { it.visitAttempt } ?: data.history.first()
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
                    (activity as? KunjunganLahanActivity)?.navigateTo(KLSection1Fragment(), 1)
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
            val kunjunganDataList = mutableListOf<PetaniKunjunganData>()

            for (petani in petaniList) {
                var allKunjungan = database.KunjunganLahanDao().getAllKunjunganForPetani(petani.idPetani, petani.namaPetani, selectedPeriode)
                val hasAuditsInSelectedPeriode = allKunjungan.isNotEmpty()

                if (allKunjungan.isEmpty()) {
                    val anyKunjungan = database.KunjunganLahanDao().getAllKunjunganForPetaniAllPeriods(petani.idPetani, petani.namaPetani)
                    if (anyKunjungan.isNotEmpty()) {
                        allKunjungan = anyKunjungan
                    }
                }

                if (allKunjungan.isEmpty()) {
                    kunjunganDataList.add(
                        PetaniKunjunganData(
                            idPetani = petani.idPetani,
                            namaPetani = petani.namaPetani,
                            desa = petani.namaDesa ?: "-",
                            statusKunjungan = "Belum Kunjungan",
                            tanggalKunjungan = "-",
                            pdfPath = "",
                            visitAttempt = 0,
                            history = emptyList()
                        )
                    )
                } else {
                    val latest = allKunjungan.maxByOrNull { it.visitAttempt } ?: allKunjungan.first()
                    val periodLabel = if (!hasAuditsInSelectedPeriode && latest.periode.isNotEmpty()) " (${latest.periode})" else ""

                    kunjunganDataList.add(
                        PetaniKunjunganData(
                            idPetani = petani.idPetani,
                            namaPetani = petani.namaPetani,
                            desa = petani.namaDesa ?: "-",
                            statusKunjungan = if (latest.statusKunjungan.isNullOrEmpty()) "Belum Kunjungan" else latest.statusKunjungan,
                            tanggalKunjungan = if (latest.tanggal.isNullOrEmpty()) "-" else latest.tanggal,
                            pdfPath = latest.pdfPath ?: "",
                            visitAttempt = latest.visitAttempt,
                            visitLabel = periodLabel,
                            history = allKunjungan.sortedByDescending { it.visitAttempt }
                        )
                    )
                }
            }

            val filteredList = if (selectedStatus == "Semua") {
                kunjunganDataList
            } else {
                kunjunganDataList.filter { it.statusKunjungan == selectedStatus }
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
