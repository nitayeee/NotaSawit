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

        val username = sharedPref.getString("username", "Admin")
        binding.username.text = "$username!"
        if (!username.isNullOrEmpty()) {
            binding.tvInitial.text = username.substring(0, 1).uppercase()
        }
        
        binding.btnProfileContainer.setOnClickListener {
            startActivity(android.content.Intent(requireContext(), com.example.notasawit.Admin.ProfilAdmin.ProfilAdminActivity::class.java))
        }

        binding.swipeRefreshBeranda.setOnRefreshListener {
            refreshAllData()
        }

        fetchDashboardData()
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

                                listPetani.add(
                                    com.example.notasawit.Room.Petani.PetaniEntity(
                                        idPetani = item.optInt("petani_id", 0),
                                        namaPetani = item.optString("petani_nama", "-"),
                                        namaDesa = namaDesa,
                                        desaId = desaId
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
        PetaniApi.getDashboardData(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonObject = JSONObject(responseData)
                        if (jsonObject.getBoolean("success")) {
                            val dataObj = jsonObject.getJSONObject("data")
                            val jumlahPetani = dataObj.getInt("jumlah_petani")
                            val jumlahLahan = dataObj.getDouble("jumlah_lahan")
                            val pemasukanArray = dataObj.getJSONArray("pemasukan")
                            val pengeluaranArray = dataObj.getJSONArray("pengeluaran")

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
                                diauditCount = statusAuditObj.optInt("pending", 0)
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
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
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
        dataSet.valueTextSize = 10f
        dataSet.lineWidth = 2.5f
        dataSet.circleRadius = 4f
        dataSet.setCircleColor(secondaryColor)
        dataSet.circleHoleColor = Color.WHITE

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
            }

            axisLeft.axisMinimum = 0f
            axisRight.isEnabled = false

            animateX(1000)
            invalidate()
        }
    }

    private fun setupPieChart(pengeluaranArray: org.json.JSONArray) {
        val entries = ArrayList<PieEntry>()
        for (i in 0 until pengeluaranArray.length()) {
            val item = pengeluaranArray.getJSONObject(i)
            entries.add(PieEntry(item.getInt("total").toFloat(), item.getString("jenis")))
        }

        val dataSet = PieDataSet(entries, "")
        val pieColors = listOf(
            Color.parseColor("#1B4D2E"), // Primary
            Color.parseColor("#D4AF37"), // Secondary
            Color.parseColor("#002617"), // Tertiary
            Color.parseColor("#638965"), // Medium Green
            Color.parseColor("#EED57B")  // Light Gold
        )
        dataSet.colors = pieColors
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE

        val data = PieData(dataSet)

        binding.pieChartPengeluaran.apply {
            this.data = data
            description.isEnabled = false
            centerText = "Pengeluaran"
            setEntryLabelColor(Color.BLACK)
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