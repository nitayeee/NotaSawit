package com.example.notasawit.Admin.Beranda

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.databinding.FragmentBerandaAdminBinding
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class BerandaAdminFragment : Fragment() {
    private var _binding: FragmentBerandaAdminBinding? = null
    private val binding get() = _binding!!

    private val sharedPref by lazy {
        requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadUserProfile()
        
        binding.btnProfileContainer.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), com.example.notasawit.Admin.ProfilAdmin.ProfilAdminActivity::class.java))
        }

        binding.swipeRefreshBeranda.setOnRefreshListener {
            refreshAllData()
        }

        fetchDashboardData()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        if (_binding == null) return
        val username = sharedPref.getString("username", sharedPref.getString("admin_username", "Admin")) ?: "Admin"
        val namaLengkap = sharedPref.getString("admin_nama", sharedPref.getString("nama", username)) ?: username
        val fotoProfil = sharedPref.getString("admin_foto", sharedPref.getString("user_profil", sharedPref.getString("foto_profil", "")))

        binding.username.text = "$username!"

        com.example.notasawit.Utils.AvatarHelper.setupAvatar(
            imageView = binding.imgProfile,
            textViewInitial = binding.tvInitial,
            nama = namaLengkap,
            fotoPathOrUrl = fotoProfil
        )

        val adminId = sharedPref.getInt("user_id", -1)
        if (adminId != -1) {
            PetaniApi.getDetailAdmin(adminId, object : Callback {
                override fun onFailure(call: Call, e: IOException) {}

                override fun onResponse(call: Call, response: Response) {
                    val responseData = response.body?.string()
                    if (response.isSuccessful && responseData != null) {
                        try {
                            val jsonObject = JSONObject(responseData)
                            if (jsonObject.getBoolean("success")) {
                                val data = jsonObject.getJSONObject("data")
                                val freshNama = data.optString("user_nama", namaLengkap)
                                val freshUsername = data.optString("user_username", username)
                                val freshFoto = data.optString("user_profil", "")

                                sharedPref.edit().apply {
                                    putString("admin_nama", freshNama)
                                    putString("admin_username", freshUsername)
                                    putString("admin_foto", freshFoto)
                                }.apply()

                                (activity as? android.app.Activity)?.runOnUiThread {
                                    if (_binding != null) {
                                        binding.username.text = "$freshUsername!"
                                        com.example.notasawit.Utils.AvatarHelper.setupAvatar(
                                            imageView = binding.imgProfile,
                                            textViewInitial = binding.tvInitial,
                                            nama = freshNama,
                                            fotoPathOrUrl = freshFoto
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            })
        }
    }

    private var pendingAdminSyncCount = 0

    private fun refreshAllData() {
        if (_binding == null) return
        binding.swipeRefreshBeranda.isRefreshing = true
        pendingAdminSyncCount = 4

        fetchDashboardData()

        val db = com.example.notasawit.Room.AppDatabase.getDatabase(requireContext())

        // 1. Sync Desa
        PetaniApi.getDesa(object : Callback {
            override fun onFailure(call: Call, e: IOException) { checkFinishAdminRefresh() }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (!json.isNullOrEmpty()) {
                        try {
                            val desaResponse = com.google.gson.Gson().fromJson(json, com.example.notasawit.Autentikasi.Daftar.DataDiri.Desa.DesaApiResponse::class.java)
                            val desaEntity = desaResponse.data.map {
                                com.example.notasawit.Room.DesaEntity(idDesa = it.desa_id, namaDesa = it.desa_nama)
                            }
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                db.masterDao().insertDesa(desaEntity)
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                checkFinishAdminRefresh()
            }
        })

        // 2. Sync Jenis Kegiatan
        PetaniApi.getJenisKegiatan(object : Callback {
            override fun onFailure(call: Call, e: IOException) { checkFinishAdminRefresh() }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (!json.isNullOrEmpty()) {
                        try {
                            val jenisResponse = com.google.gson.Gson().fromJson(json, com.example.notasawit.InputKegiatan.JenisKegiatan.JenisKegiatanApiResponse::class.java)
                            val jenisEntity = jenisResponse.data.map {
                                com.example.notasawit.Room.JenisKegiatan.JenisKegiatanEntity(id_jenis = it.id_jenis, nama_jenis = it.nama_jenis, ikon = it.ikon)
                            }
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                db.JenisKegiatanDao().insertJenisKegiatan(jenisEntity)
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                checkFinishAdminRefresh()
            }
        })

        // 3. Sync Admins / Auditors
        PetaniApi.getAdmins(object : Callback {
            override fun onFailure(call: Call, e: IOException) { checkFinishAdminRefresh() }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        try {
                            val jsonObject = JSONObject(body)
                            val dataArray = jsonObject.getJSONArray("data")
                            val listAuditor = mutableListOf<com.example.notasawit.Room.Auditor.AuditorEntity>()
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                listAuditor.add(com.example.notasawit.Room.Auditor.AuditorEntity(
                                    idAuditor = item.getInt("user_id"),
                                    namaAuditor = item.getString("user_nama"),
                                    username = item.getString("user_username")
                                ))
                            }
                            if (listAuditor.isNotEmpty()) {
                                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    db.masterDao().insertAuditor(listAuditor)
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                checkFinishAdminRefresh()
            }
        })

        // 4. Sync Petani
        PetaniApi.getAllPetani(object : Callback {
            override fun onFailure(call: Call, e: IOException) { checkFinishAdminRefresh() }
            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    if (response.isSuccessful && !json.isNullOrEmpty()) {
                        try {
                            val jsonObject = JSONObject(json)
                            val dataArray = jsonObject.getJSONArray("data")
                            val listPetani = mutableListOf<com.example.notasawit.Room.Petani.PetaniEntity>()

                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val desaId = item.optInt("desa_id", 0)
                                val namaDesa = item.optJSONObject("desa")?.optString("desa_nama", "-") ?: "-"
                                val foto = item.optString("petani_foto", item.optString("user_profil", item.optString("foto", item.optString("profil_petani", ""))))

                                listPetani.add(
                                    com.example.notasawit.Room.Petani.PetaniEntity(
                                        idPetani = item.optInt("petani_id", 0),
                                        namaPetani = item.optString("petani_nama", "-"),
                                        namaDesa = namaDesa,
                                        desaId = desaId,
                                        fotoProfil = if (foto.isNullOrEmpty() || foto == "null") null else foto
                                    )
                                )
                            }
                            db.masterDao().deleteAllPetani()
                            if (listPetani.isNotEmpty()) {
                                db.masterDao().insertPetani(listPetani)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    checkFinishAdminRefresh()
                }
            }
        })
    }

    @Synchronized
    private fun checkFinishAdminRefresh() {
        pendingAdminSyncCount--
        if (pendingAdminSyncCount <= 0) {
            activity?.runOnUiThread {
                _binding?.swipeRefreshBeranda?.isRefreshing = false
            }
        }
    }

    private fun fetchDashboardData() {
        val adminDesaId = sharedPref.getInt("admin_desa_id", 0)

        PetaniApi.getDashboardData(adminDesaId, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                computeStatusAuditFromRoom(adminDesaId)
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonObject = JSONObject(responseData)
                        if (jsonObject.getBoolean("success")) {
                            val dataObj = jsonObject.getJSONObject("data")
                            val jumlahPetani = dataObj.optInt("jumlah_petani", 0)
                            val jumlahLahan = dataObj.optDouble("jumlah_lahan", 0.0)
                            val pemasukanArray = dataObj.optJSONArray("pemasukan") ?: org.json.JSONArray()
                            val pengeluaranArray = dataObj.optJSONArray("pengeluaran") ?: org.json.JSONArray()

                            val pengingatList = mutableListOf<Pengingat>()
                            if (dataObj.has("pengingat")) {
                                val pengingatArray = dataObj.getJSONArray("pengingat")
                                for (i in 0 until pengingatArray.length()) {
                                    val item = pengingatArray.getJSONObject(i)
                                    pengingatList.add(
                                        Pengingat(
                                            item.getInt("id"),
                                            item.getString("judul"),
                                            item.getString("pesan"),
                                            item.getString("deadline"),
                                            if (item.has("is_done")) item.getBoolean("is_done") else false
                                        )
                                    )
                                }
                            }
                            
                            var lulusCount = 0
                            var perbaikanCount = 0
                            var diauditCount = 0
                            if (dataObj.has("status_audit")) {
                                val statusAuditObj = dataObj.getJSONObject("status_audit")
                                lulusCount = statusAuditObj.optInt("lulus", 0)
                                perbaikanCount = statusAuditObj.optInt("perlu_perbaikan", 0)
                                diauditCount = statusAuditObj.optInt("pending", statusAuditObj.optInt("perlu_audit", 0))
                            }

                            activity?.runOnUiThread {
                                binding.tvJumlahPetani.text = jumlahPetani.toString()
                                binding.tvLuasLahan.text = String.format("%.2f", jumlahLahan)
                                binding.tvLulusCount.text = lulusCount.toString()
                                binding.tvPerbaikanCount.text = perbaikanCount.toString()
                                binding.tvDiauditCount.text = diauditCount.toString()
                                setupLineChart(pemasukanArray)
                                setupPieChart(pengeluaranArray)
                                setupPengingatList(pengingatList)
                            }

                            computeStatusAuditFromRoom(adminDesaId)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        computeStatusAuditFromRoom(adminDesaId)
                    }
                } else {
                    computeStatusAuditFromRoom(adminDesaId)
                }
            }
        })
    }

    private fun computeStatusAuditFromRoom(adminDesaId: Int) {
        if (_binding == null) return
        val db = com.example.notasawit.Room.AppDatabase.getDatabase(requireContext())
        lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val petaniList = if (adminDesaId != 0) {
                    db.masterDao().getPetaniByDesa(adminDesaId)
                } else {
                    db.masterDao().getAllPetani()
                }

                if (petaniList.isNotEmpty()) {
                    var lulus = 0
                    var perbaikan = 0
                    var pending = 0

                    val allAudits = db.auditDao().getAllAuditHeaders()

                    for (petani in petaniList) {
                        val farmerAudits = allAudits.filter { 
                            it.idPetani == petani.idPetani || it.namaPetani.equals(petani.namaPetani, ignoreCase = true)
                        }.sortedByDescending { it.auditAttempt }

                        val lastAudit = farmerAudits.firstOrNull()
                        if (lastAudit != null) {
                            when {
                                lastAudit.statusAudit.equals("Lulus", ignoreCase = true) -> lulus++
                                lastAudit.statusAudit.equals("Perlu Perbaikan", ignoreCase = true) -> perbaikan++
                                else -> pending++
                            }
                        } else {
                            pending++
                        }
                    }

                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        _binding?.let { b ->
                            b.tvJumlahPetani.text = petaniList.size.toString()
                            b.tvLulusCount.text = lulus.toString()
                            b.tvPerbaikanCount.text = perbaikan.toString()
                            b.tvDiauditCount.text = pending.toString()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupLineChart(pemasukanArray: org.json.JSONArray) {
        val entries = ArrayList<Entry>()
        for (i in 0 until pemasukanArray.length()) {
            entries.add(Entry(i.toFloat(), pemasukanArray.getInt(i).toFloat()))
        }

        val dataSet = LineDataSet(entries, "Pemasukan")
        val primaryColor = Color.parseColor("#1B4D2E")
        val secondaryColor = Color.parseColor("#D4AF37")

        dataSet.color = primaryColor
        dataSet.lineWidth = 2.5f
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(secondaryColor)
        dataSet.circleHoleColor = Color.WHITE
        dataSet.setDrawValues(false) // Disable messy overlapping numbers on top of nodes
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER // Smooth curved line
        dataSet.setDrawFilled(true)
        dataSet.fillColor = primaryColor
        dataSet.fillAlpha = 20

        val data = LineData(dataSet)
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agt", "Sep", "Okt", "Nov", "Des")

        binding.lineChartPemasukan.apply {
            this.data = data
            description.isEnabled = false

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(months)
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                isGranularityEnabled = true
                textColor = Color.parseColor("#718096")
            }

            axisLeft.apply {
                axisMinimum = 0f
                spaceTop = 20f // Give margin at top so line/values don't clip
                textColor = Color.parseColor("#718096")
                setDrawGridLines(true)
                gridColor = Color.parseColor("#EDF2F7")
            }

            axisRight.isEnabled = false
            legend.isEnabled = false
            animateX(1000)
            invalidate()
        }
    }

    private fun setupPieChart(pengeluaranArray: org.json.JSONArray) {
        val entries = ArrayList<PieEntry>()
        var totalAmount = 0f

        for (i in 0 until pengeluaranArray.length()) {
            val item = pengeluaranArray.getJSONObject(i)
            val valFloat = item.getInt("total").toFloat()
            totalAmount += valFloat
            entries.add(PieEntry(valFloat, item.getString("jenis")))
        }

        val dataSet = PieDataSet(entries, "")
        val pieColors = listOf(
            Color.parseColor("#1B4D2E"), // Primary Green
            Color.parseColor("#D4AF37"), // Secondary Gold
            Color.parseColor("#2C6E49"), // Forest Green
            Color.parseColor("#4C956C"), // Sage Green
            Color.parseColor("#EED57B"), // Soft Gold
            Color.parseColor("#A4C3B2")  // Muted Teal
        )
        dataSet.colors = pieColors
        dataSet.valueTextSize = 11f
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTypeface = android.graphics.Typeface.DEFAULT_BOLD
        dataSet.valueFormatter = PercentFormatter(binding.pieChartPengeluaran)
        dataSet.sliceSpace = 2f

        val data = PieData(dataSet)

        binding.pieChartPengeluaran.apply {
            this.data = data
            description.isEnabled = false
            setUsePercentValues(true)
            setDrawEntryLabels(false) // Disable overlapping category text on pie slices!
            
            holeRadius = 55f
            transparentCircleRadius = 60f
            setHoleColor(Color.WHITE)

            centerText = "Pengeluaran"
            setCenterTextSize(13f)
            setCenterTextColor(Color.parseColor("#1B4D2E"))
            setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD)

            legend.apply {
                isEnabled = true
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                isWordWrapEnabled = true
                textSize = 11f
                form = Legend.LegendForm.CIRCLE
                formSize = 8f
                xEntrySpace = 12f
                yEntrySpace = 6f
                textColor = Color.parseColor("#4A5568")
            }

            animateY(1000)
            invalidate()
        }
    }

    private fun setupPengingatList(pengingatList: List<Pengingat>) {
        if (pengingatList.isEmpty()) {
            binding.rvPengingat.visibility = View.GONE
            binding.tvEmptyPengingat.visibility = View.VISIBLE
        } else {
            binding.rvPengingat.visibility = View.VISIBLE
            binding.tvEmptyPengingat.visibility = View.GONE
            
            val adapter = PengingatAdapter(pengingatList)
            binding.rvPengingat.layoutManager = LinearLayoutManager(requireContext())
            binding.rvPengingat.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}