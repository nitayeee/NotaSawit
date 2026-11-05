package com.example.notasawit.Home

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.notasawit.InputKegiatan.InputKegiatanActivity
import com.example.notasawit.Pemasukan.InputPemasukanActivity
import com.example.notasawit.databinding.FragmentBerandaBinding
import com.example.notasawit.Model.QuoteResponse
import com.example.notasawit.Network.RetrofitClient
import com.example.notasawit.Pengeluaran.InputPengeluaranActivity

import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import org.json.JSONObject
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaBinding? = null
    private val binding get() = _binding!!

    private val handler = Handler(Looper.getMainLooper())
    private val sharedPref by lazy {
        requireActivity().getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBerandaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)
        val username = sharedPref.getString("username", "") ?: ""
        binding.username.setText("$username!")
        binding.username.setTextColor(Color.parseColor("#1B4D2E"))
        database = AppDatabase.getDatabase(requireContext())
        fetchHargaTbs()

        binding.swipeRefresh.setOnRefreshListener {
            refreshAllDataFromSync()
        }



        binding.cardInputKegiatan.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    InputKegiatanActivity::class.java
                )
            )
        }

        binding.cardPemasukan.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    InputPemasukanActivity::class.java
                )
            )
        }
        binding.cardPengeluaran.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    InputPengeluaranActivity::class.java
                )
            )
        }
        
        binding.cardTotalPanen.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    com.example.notasawit.Home.Panen.GrafikPanenActivity::class.java
                )
            )
        }

        binding.cardHargaTbs.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    com.example.notasawit.Admin.HargaTbs.RiwayatHargaTbsActivity::class.java
                )
            )
        }

        binding.btnProfileContainer.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    com.example.notasawit.ProfilPetani.ProfilPetaniActivity::class.java
                )
            )
        }
        
        binding.cardEdukasi.setOnClickListener {
            startActivity(
                Intent(
                    requireContext(),
                    EdukasiActivity::class.java
                )
            )
        }
        updateProfileUI()

        val petaniId = sharedPref.getInt("petani_id", -1)
        if (petaniId != -1) {
            loadPetaniSummary(petaniId)
            loadTotalPanenBulanan(petaniId)
            checkTahunTanamLahan(petaniId)
        }
        
        binding.btnIsiTahunTanam.setOnClickListener {
            startActivity(Intent(requireContext(), com.example.notasawit.ProfilPetani.EditLahanActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        updateProfileUI()
        val petaniId = sharedPref.getInt("petani_id", -1)
        if (petaniId != -1) {
            loadTotalPanenBulanan(petaniId)
            checkTahunTanamLahan(petaniId)
        }
    }

    private fun updateProfileUI() {
        val fotoProfil = sharedPref.getString("profilPetani", null)
        val namaPetani = sharedPref.getString("namaPetani", "") ?: ""
        
        if (fotoProfil.isNullOrBlank() || fotoProfil == "null") {
            binding.imgProfile.visibility = View.GONE
            binding.tvInitial.visibility = View.VISIBLE
            binding.tvInitial.text = getInitials(namaPetani)
        } else {
            binding.tvInitial.visibility = View.GONE
            binding.imgProfile.visibility = View.VISIBLE
            val fullUrl = if (!fotoProfil.startsWith("http")) {
                "https://notasawit.pocari.id/storage/$fotoProfil"
            } else {
                fotoProfil
            }
            Glide.with(this)
                .load(fullUrl)
                .into(binding.imgProfile)
        }
    }


    fun getInitials(name: String): String {
        return com.example.notasawit.Utils.AvatarHelper.getInitials(name)
    }

    private fun loadPetaniSummary(petaniId: Int) {
        com.example.notasawit.Network.PetaniApi.getPetaniSummary(petaniId, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                if (!isAdded || _binding == null) return
                requireActivity().runOnUiThread {
                    binding.tvPemasukan.text = "Rp 0"
                    binding.tvPengeluaran.text = "Rp 0"
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!isAdded || _binding == null) return
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonObject = org.json.JSONObject(responseData)
                        if (jsonObject.getBoolean("success")) {
                            val dataObj = jsonObject.getJSONObject("data")
                            val pemasukanIni = dataObj.getDouble("pemasukan_bulan_ini")
                            val pemasukanLalu = dataObj.getDouble("pemasukan_bulan_lalu")
                            val pengeluaranIni = dataObj.getDouble("pengeluaran_bulan_ini")
                            val pengeluaranLalu = dataObj.getDouble("pengeluaran_bulan_lalu")

                            requireActivity().runOnUiThread {
                                binding.tvPemasukan.text = formatCurrency(pemasukanIni)
                                binding.tvPengeluaran.text = formatCurrency(pengeluaranIni)

                                // Logic Pemasukan
                                if (pemasukanIni > pemasukanLalu) {
                                    binding.tvBadgePemasukan.text = "↑ Meningkat"
                                    binding.tvBadgePemasukan.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_green)
                                    binding.tvBadgePemasukan.setTextColor(Color.parseColor("#1B4D2E"))
                                } else if (pemasukanIni < pemasukanLalu) {
                                    binding.tvBadgePemasukan.text = "↓ Menurun"
                                    binding.tvBadgePemasukan.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_red)
                                    binding.tvBadgePemasukan.setTextColor(Color.parseColor("#C62828"))
                                } else {
                                    binding.tvBadgePemasukan.text = "Stabil"
                                    binding.tvBadgePemasukan.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_green)
                                    binding.tvBadgePemasukan.setTextColor(Color.parseColor("#1B4D2E"))
                                }

                                // Logic Pengeluaran
                                if (pengeluaranIni < pengeluaranLalu) {
                                    binding.tvBadgePengeluaran.text = "↓ Lebih hemat"
                                    binding.tvBadgePengeluaran.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_green)
                                    binding.tvBadgePengeluaran.setTextColor(Color.parseColor("#1B4D2E"))
                                } else if (pengeluaranIni > pengeluaranLalu) {
                                    binding.tvBadgePengeluaran.text = "↑ Lebih boros"
                                    binding.tvBadgePengeluaran.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_red)
                                    binding.tvBadgePengeluaran.setTextColor(Color.parseColor("#C62828"))
                                } else {
                                    binding.tvBadgePengeluaran.text = "Stabil"
                                    binding.tvBadgePengeluaran.setBackgroundResource(com.example.notasawit.R.drawable.bg_badge_green)
                                    binding.tvBadgePengeluaran.setTextColor(Color.parseColor("#1B4D2E"))
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

    private fun formatCurrency(amount: Double): String {
        val format = java.text.NumberFormat.getCurrencyInstance(java.util.Locale("id", "ID"))
        format.maximumFractionDigits = 0
        return format.format(amount).replace("Rp", "Rp ")
    }


//    override fun onResume() {
//        super.onResume()
//
//        lifecycleScope.launch {
//
//            SyncProduksiRepository(
//                requireContext(),
//                database
//            ).syncProduksi()
//
//    }

    private fun checkTahunTanamLahan(petaniId: Int) {
        com.example.notasawit.Network.PetaniApi.getLahanByPetani(petaniId, object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                // Ignore failure
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (!isAdded || _binding == null) return
                val responseData = response.body?.string()
                
                var hasEmptyTahunTanam = false
                
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonObject = org.json.JSONObject(responseData)
                        if (jsonObject.optBoolean("success", false)) {
                            val dataArray = jsonObject.optJSONArray("data")
                            if (dataArray != null && dataArray.length() > 0) {
                                val lahanList = mutableListOf<com.example.notasawit.Room.Lahan.LahanEntity>()
                                for (i in 0 until dataArray.length()) {
                                    val lahanObj = dataArray.getJSONObject(i)
                                    
                                    val lahanId = lahanObj.optInt("lahan_id", 0)
                                    val lahanNama = lahanObj.optString("lahan_nama", "")
                                    val lahanLuas = if (lahanObj.has("lahan_luas") && !lahanObj.isNull("lahan_luas")) {
                                        lahanObj.optDouble("lahan_luas", 0.0)
                                    } else if (lahanObj.has("luas_lahan") && !lahanObj.isNull("luas_lahan")) {
                                        lahanObj.optDouble("luas_lahan", 0.0)
                                    } else {
                                        0.0
                                    }
                                    lahanList.add(
                                        com.example.notasawit.Room.Lahan.LahanEntity(
                                            lahan_id = lahanId,
                                            petani_id = petaniId,
                                            lahan_nama = lahanNama,
                                            lahan_luas = lahanLuas
                                        )
                                    )
                                    
                                    val tahunTanam = lahanObj.optString("tahun_tanam", "")
                                    if (tahunTanam.isNullOrEmpty() || tahunTanam == "null" || tahunTanam == "0") {
                                        hasEmptyTahunTanam = true
                                        // Jangan break agar semua lahan tersimpan di lokal database
                                    }
                                }
                                
                                if (lahanList.isNotEmpty()) {
                                    lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                        database.LahanDao().insertLahan(lahanList)
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                requireActivity().runOnUiThread {
                    if (hasEmptyTahunTanam) {
                        binding.cardAlertTahunTanam.visibility = android.view.View.VISIBLE
                    } else {
                        binding.cardAlertTahunTanam.visibility = android.view.View.GONE
                    }
                }
            }
        })
    }

    private var pendingSyncCount = 0

    private fun refreshAllDataFromSync() {
        if (_binding == null) return
        binding.swipeRefresh.isRefreshing = true
        pendingSyncCount = 3
        val petaniId = sharedPref.getInt("petani_id", -1)

        fetchHargaTbs()
        if (petaniId != -1) {
            loadPetaniSummary(petaniId)
            checkTahunTanamLahan(petaniId)
        }

        syncDesaFromApi()
        syncJenisKegiatanFromApi()
        syncAdminsAndPetaniFromApi()
    }

    @Synchronized
    private fun checkFinishRefreshing() {
        pendingSyncCount--
        if (pendingSyncCount <= 0) {
            activity?.runOnUiThread {
                _binding?.swipeRefresh?.isRefreshing = false
            }
        }
    }

    private fun syncDesaFromApi() {
        com.example.notasawit.Network.PetaniApi.getDesa(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                checkFinishRefreshing()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (!json.isNullOrEmpty()) {
                        try {
                            val desaResponse = com.google.gson.Gson().fromJson(json, com.example.notasawit.Autentikasi.Daftar.DataDiri.Desa.DesaApiResponse::class.java)
                            val desaEntity = desaResponse.data.map {
                                com.example.notasawit.Room.DesaEntity(idDesa = it.desa_id, namaDesa = it.desa_nama)
                            }
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                database.masterDao().insertDesa(desaEntity)
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                checkFinishRefreshing()
            }
        })
    }

    private fun syncJenisKegiatanFromApi() {
        com.example.notasawit.Network.PetaniApi.getJenisKegiatan(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                checkFinishRefreshing()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    if (!json.isNullOrEmpty()) {
                        try {
                            val jenisResponse = com.google.gson.Gson().fromJson(json, com.example.notasawit.InputKegiatan.JenisKegiatan.JenisKegiatanApiResponse::class.java)
                            val jenisEntity = jenisResponse.data.map {
                                com.example.notasawit.Room.JenisKegiatan.JenisKegiatanEntity(id_jenis = it.id_jenis, nama_jenis = it.nama_jenis, ikon = it.ikon)
                            }
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                database.JenisKegiatanDao().insertJenisKegiatan(jenisEntity)
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                checkFinishRefreshing()
            }
        })
    }

    private fun syncAdminsAndPetaniFromApi() {
        com.example.notasawit.Network.PetaniApi.getAdmins(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {}
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        try {
                            val jsonObject = org.json.JSONObject(body)
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
                                    database.masterDao().insertAuditor(listAuditor)
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
            }
        })

        com.example.notasawit.Network.PetaniApi.getAllPetani(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                checkFinishRefreshing()
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (!body.isNullOrEmpty()) {
                        try {
                            val jsonObject = org.json.JSONObject(body)
                            val dataArray = jsonObject.getJSONArray("data")
                            val listPetani = mutableListOf<com.example.notasawit.Room.Petani.PetaniEntity>()
                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                val desaId = item.optInt("desa_id", 0)
                                val foto = item.optString("petani_foto", item.optString("user_profil", item.optString("foto", item.optString("profil_petani", ""))))

                                val namaDesa = if (item.has("desa") && !item.isNull("desa")) {
                                    item.getJSONObject("desa").optString("nama_desa", "-")
                                } else {
                                    item.optString("petani_username", "-")
                                }

                                listPetani.add(com.example.notasawit.Room.Petani.PetaniEntity(
                                    idPetani = item.optInt("petani_id", 0),
                                    namaPetani = item.optString("petani_nama", "-"),
                                    namaDesa = namaDesa,
                                    desaId = desaId,
                                    fotoProfil = if (foto.isNullOrEmpty() || foto == "null") null else foto
                                ))
                            }
                            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                database.masterDao().deleteAllPetani()
                                if (listPetani.isNotEmpty()) {
                                    database.masterDao().insertPetani(listPetani)
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                checkFinishRefreshing()
            }
        })
    }

    private fun fetchHargaTbs() {
        PetaniApi.getHargaTbs(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && !responseData.isNullOrEmpty()) {
                    try {
                        val json = JSONObject(responseData)
                        val data = if (json.has("data")) json.optJSONObject("data") else json
                        if (data != null) {
                            val rawDinas = data.optString("harga_dinas", "0")
                            val hargaDinas = rawDinas.toDoubleOrNull() ?: data.optDouble("harga_dinas", 0.0)

                            val rawPtSar = data.optString("harga_pt_sar", "0")
                            val hargaPtSar = rawPtSar.toDoubleOrNull() ?: data.optDouble("harga_pt_sar", 0.0)

                            val tgl = data.optString("tanggal_berlaku", data.optString("created_at", "Terbaru"))

                            val fmtDinas = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(hargaDinas.toLong())
                            val fmtPtSar = java.text.NumberFormat.getNumberInstance(java.util.Locale("id", "ID")).format(hargaPtSar.toLong())

                            activity?.runOnUiThread {
                                if (_binding != null) {
                                    binding.tvHargaDinas.text = "Rp $fmtDinas / Kg"
                                    binding.tvHargaPtSar.text = "Rp $fmtPtSar / Kg"
                                    binding.tvTanggalBerlakuTbs.text = "Tmt: $tgl"
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

    private fun loadTotalPanenBulanan(petaniId: Int) {
        if (_binding == null) return

        val calendar = java.util.Calendar.getInstance()
        val currentYear = calendar.get(java.util.Calendar.YEAR)
        val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
        val currentMonthKey = String.format(java.util.Locale.US, "%04d-%02d", currentYear, currentMonth)

        val calPrev = java.util.Calendar.getInstance().apply { add(java.util.Calendar.MONTH, -1) }
        val prevYear = calPrev.get(java.util.Calendar.YEAR)
        val prevMonth = calPrev.get(java.util.Calendar.MONTH) + 1
        val prevMonthKey = String.format(java.util.Locale.US, "%04d-%02d", prevYear, prevMonth)

        val monthFormat = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale("id", "ID"))
        binding.tvBulanPanen.text = monthFormat.format(calendar.time)

        // Tampilkan cache cepat dari SharedPreferences jika ada
        val cachedKg = sharedPref.getFloat("cached_panen_bulan_ini", -1f)
        if (cachedKg >= 0f) {
            binding.tvTotalPanen.text = formatPanenWeight(cachedKg.toDouble())
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // 1. Ambil data lokal yang belum disinkron dari Room
            val localUnsynced = try {
                database.ProduksiDao().getAll()
            } catch (e: Exception) {
                emptyList()
            }

            var localBulanIni = 0.0
            var localBulanLalu = 0.0

            localUnsynced.forEach {
                val tgl = it.produksi_tanggal
                if (tgl.startsWith(currentMonthKey)) {
                    localBulanIni += it.jumlah_tbs
                } else if (tgl.startsWith(prevMonthKey)) {
                    localBulanLalu += it.jumlah_tbs
                }
            }

            // 2. Ambil data riwayat pemasukan produksi dari API
            com.example.notasawit.Network.PetaniApi.getRiwayatKeuangan(
                petaniId = petaniId,
                bulan = null,
                tahun = null,
                lahanId = null,
                tipe = "pemasukan",
                callback = object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        if (!isAdded || _binding == null) return
                        requireActivity().runOnUiThread {
                            updateTotalPanenUI(localBulanIni, localBulanLalu)
                        }
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        if (!isAdded || _binding == null) return
                        val body = response.body?.string() ?: ""
                        var apiBulanIni = 0.0
                        var apiBulanLalu = 0.0

                        try {
                            val json = org.json.JSONObject(body)
                            val data = json.optJSONArray("data")
                            if (data != null) {
                                for (i in 0 until data.length()) {
                                    val item = data.optJSONObject(i) ?: continue
                                    val tgl = item.optString("tanggal", "")
                                    val jumlah = when {
                                        item.has("jumlah_tbs") && !item.isNull("jumlah_tbs") -> {
                                            item.optString("jumlah_tbs").toDoubleOrNull() ?: item.optDouble("jumlah_tbs", 0.0)
                                        }
                                        item.has("jumlah") && !item.isNull("jumlah") -> {
                                            item.optString("jumlah").toDoubleOrNull() ?: item.optDouble("jumlah", 0.0)
                                        }
                                        else -> 0.0
                                    }

                                    if (tgl.startsWith(currentMonthKey)) {
                                        apiBulanIni += jumlah
                                    } else if (tgl.startsWith(prevMonthKey)) {
                                        apiBulanLalu += jumlah
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("BERANDA_PANEN", "Error parsing panen: ${e.message}")
                        }

                        val totalIni = apiBulanIni + localBulanIni
                        val totalLalu = apiBulanLalu + localBulanLalu

                        sharedPref.edit().putFloat("cached_panen_bulan_ini", totalIni.toFloat()).apply()

                        if (isAdded && _binding != null) {
                            requireActivity().runOnUiThread {
                                updateTotalPanenUI(totalIni, totalLalu)
                            }
                        }
                    }
                }
            )
        }
    }

    private fun updateTotalPanenUI(totalBulanIni: Double, totalBulanLalu: Double) {
        if (_binding == null) return
        binding.tvTotalPanen.text = formatPanenWeight(totalBulanIni)

        if (totalBulanLalu > 0.0) {
            val diff = ((totalBulanIni - totalBulanLalu) / totalBulanLalu) * 100.0
            if (diff > 0.0) {
                binding.tvPerbandinganPanen.text = "↑ +${String.format(java.util.Locale("id", "ID"), "%.0f", diff)}% dari bulan lalu"
                binding.tvPerbandinganPanen.setTextColor(Color.parseColor("#D1FAE5"))
            } else if (diff < 0.0) {
                binding.tvPerbandinganPanen.text = "↓ ${String.format(java.util.Locale("id", "ID"), "%.0f", diff)}% dari bulan lalu"
                binding.tvPerbandinganPanen.setTextColor(Color.parseColor("#FED7AA"))
            } else {
                binding.tvPerbandinganPanen.text = "Stabil dari bulan lalu"
                binding.tvPerbandinganPanen.setTextColor(Color.parseColor("#D1FAE5"))
            }
        } else {
            if (totalBulanIni > 0.0) {
                binding.tvPerbandinganPanen.text = "↑ Panen tercatat bulan ini"
                binding.tvPerbandinganPanen.setTextColor(Color.parseColor("#D1FAE5"))
            } else {
                binding.tvPerbandinganPanen.text = "Belum ada catatan panen bulan ini"
                binding.tvPerbandinganPanen.setTextColor(Color.parseColor("#E2E8F0"))
            }
        }
    }

    private fun formatPanenWeight(kg: Double): String {
        val df = java.text.DecimalFormat("#,##0.#", java.text.DecimalFormatSymbols(java.util.Locale("id", "ID")))
        return if (kg >= 1000.0) {
            "${df.format(kg / 1000.0)} Ton"
        } else {
            "${df.format(kg)} Kg"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}