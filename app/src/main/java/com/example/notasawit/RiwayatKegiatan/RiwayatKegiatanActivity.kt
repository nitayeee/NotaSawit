package com.example.notasawit.RiwayatKegiatan

import com.example.notasawit.InputKegiatan.InputKegiatanActivity
import com.example.notasawit.InputKegiatan.KegiatanAdapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.KegiatanPetani.KegiatanEntity
import com.example.notasawit.Room.Lahan.LahanEntity
import com.example.notasawit.databinding.ActivityRiwayatKegiatanBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.io.IOException
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject


class RiwayatKegiatanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiwayatKegiatanBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: KegiatanAdapter

    private val allItemsWithLahan = mutableListOf<KegiatanAdapter.ItemWithLahan>()
    private val filteredItems = mutableListOf<KegiatanAdapter.ItemWithLahan>()
    private val listLahan = mutableListOf<LahanEntity>()

    private val bulanList = listOf(
        "Semua Bulan",
        "Januari",
        "Februari",
        "Maret",
        "April",
        "Mei",
        "Juni",
        "Juli",
        "Agustus",
        "September",
        "Oktober",
        "November",
        "Desember"
    )
    private val tahunList = mutableListOf<String>()
    private val lahanListDropdown = mutableListOf<String>()

    private var bulanAdapter: ArrayAdapter<String>? = null
    private var tahunAdapter: ArrayAdapter<String>? = null
    private var lahanAdapter: ArrayAdapter<String>? = null

    private val sharedPref by lazy {
        getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE)
    }

    private val spPetaniId: Int
        get() {
            val id = sharedPref.getInt("petani_id", 0)
            if (id != 0) return id
            val uid = sharedPref.getInt("user_id", 0)
            if (uid != 0) return uid
            val strId = sharedPref.getString("petani_id", null)?.toIntOrNull()
            if (strId != null && strId != 0) return strId
            val strUid = sharedPref.getString("user_id", null)?.toIntOrNull()
            if (strUid != null && strUid != 0) return strUid
            return 0
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRiwayatKegiatanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = AppDatabase.getDatabase(this)

        setupRecyclerView()
        setupListeners()
        setupDropdownFilters()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = KegiatanAdapter(this, mutableListOf()) { item ->
            showDetailDialog(item)
        }
        binding.rvKegiatan.layoutManager = LinearLayoutManager(this)
        binding.rvKegiatan.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnInputBaru?.setOnClickListener {
            startActivity(Intent(this, InputKegiatanActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadData()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupDropdownFilters() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)

        // 1. Bulan Dropdown (Default: "Semua Bulan")
        bulanAdapter = ArrayAdapter(this, R.layout.item_spinner_filter, bulanList).apply {
            setDropDownViewResource(R.layout.item_spinner_filter_dropdown)
        }
        binding.spBulan.adapter = bulanAdapter
        binding.spBulan.setSelection(0)
        binding.spBulan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 2. Tahun Dropdown (Default: Tahun yang sedang berjalan)
        val initialYears = listOf("Semua Tahun") + (currentYear downTo (currentYear - 3)).map { it.toString() }
        tahunList.clear()
        tahunList.addAll(initialYears)
        tahunAdapter = ArrayAdapter(this, R.layout.item_spinner_filter, tahunList).apply {
            setDropDownViewResource(R.layout.item_spinner_filter_dropdown)
        }
        binding.spTahun.adapter = tahunAdapter
        val defaultYearIndex = tahunList.indexOf(currentYear.toString())
        if (defaultYearIndex >= 0) {
            binding.spTahun.setSelection(defaultYearIndex)
        }
        binding.spTahun.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 3. Lahan Dropdown (Default: "Semua Lahan")
        lahanListDropdown.clear()
        lahanListDropdown.add("Semua Lahan")
        lahanAdapter = ArrayAdapter(this, R.layout.item_spinner_filter, lahanListDropdown).apply {
            setDropDownViewResource(R.layout.item_spinner_filter_dropdown)
        }
        binding.spLahan.adapter = lahanAdapter
        binding.spLahan.setSelection(0)
        binding.spLahan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateLahanDropdown(allLahan: List<LahanEntity>) {
        val currentSelection = binding.spLahan.selectedItem?.toString() ?: "Semua Lahan"
        val newLahanNames = mutableListOf("Semua Lahan")
        newLahanNames.addAll(allLahan.map { it.lahan_nama }.distinct())

        lahanListDropdown.clear()
        lahanListDropdown.addAll(newLahanNames)
        lahanAdapter?.notifyDataSetChanged()

        val pos = lahanListDropdown.indexOf(currentSelection)
        if (pos >= 0) {
            binding.spLahan.setSelection(pos)
        } else {
            binding.spLahan.setSelection(0)
        }
    }

    private fun updateYearDropdown() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val currentSelection = binding.spTahun.selectedItem?.toString() ?: currentYear.toString()

        val extractedYears = allItemsWithLahan.mapNotNull {
            parseDateYearMonth(it.kegiatan.kegiatan_tanggal).first
        }.distinct()

        val allYears = (extractedYears + (currentYear downTo (currentYear - 3))).distinct().sortedDescending()
        val newYears = listOf("Semua Tahun") + allYears.map { it.toString() }

        tahunList.clear()
        tahunList.addAll(newYears)
        tahunAdapter?.notifyDataSetChanged()

        val pos = tahunList.indexOf(currentSelection)
        if (pos >= 0) {
            binding.spTahun.setSelection(pos)
        } else {
            val curPos = tahunList.indexOf(currentYear.toString())
            binding.spTahun.setSelection(if (curPos >= 0) curPos else 0)
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun loadUserLahan(petaniId: Int) {
        if (petaniId == 0) return
        PetaniApi.getLahanByPetani(petaniId, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // Ignore failure, rely on local data
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string() ?: return
                if (!response.isSuccessful) return
                try {
                    val lahanList = mutableListOf<LahanEntity>()
                    val trimmed = responseData.trim()
                    val dataArray = if (trimmed.startsWith("[")) {
                        JSONArray(trimmed)
                    } else {
                        val jsonObject = JSONObject(trimmed)
                        jsonObject.optJSONArray("data")
                    }

                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val obj = dataArray.getJSONObject(i)
                            val id = obj.optInt("lahan_id", obj.optInt("id", 0))
                            val nama = obj.optString("lahan_nama", obj.optString("nama", ""))
                            val luas = obj.optDouble("lahan_luas", obj.optDouble("luas_lahan", 0.0))
                            if (id != 0 && nama.isNotEmpty()) {
                                lahanList.add(
                                    LahanEntity(
                                        lahan_id = id,
                                        petani_id = petaniId,
                                        lahan_nama = nama,
                                        lahan_luas = luas
                                    )
                                )
                            }
                        }
                    }

                    if (lahanList.isNotEmpty()) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            database.LahanDao().insertLahan(lahanList)
                        }
                        runOnUiThread {
                            listLahan.clear()
                            listLahan.addAll(lahanList)
                            updateLahanDropdown(lahanList)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LAHAN_LOAD", "Error parsing lahan: ${e.message}")
                }
            }
        })
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true

        triggerKegiatanSync()

        val currentPetaniId = spPetaniId
        if (currentPetaniId != 0 && isOnline()) {
            loadUserLahan(currentPetaniId)
        }

        lifecycleScope.launch {
            try {
                // 1. Load data lokal (yang belum tersinkron) dari Room DB
                val rawLocalKegiatan = database.KegiatanDao().getAll()
                val localKegiatanFiltered = if (currentPetaniId != 0) {
                    val list = rawLocalKegiatan.filter { it.petani_id == currentPetaniId }
                    if (list.isNotEmpty()) list else rawLocalKegiatan
                } else {
                    rawLocalKegiatan
                }

                val allLahan = database.LahanDao().getAllLahan()
                val userLahan = if (currentPetaniId != 0) {
                    val byPetani = database.LahanDao().getLahanByPetani(currentPetaniId)
                    if (byPetani.isNotEmpty()) byPetani else allLahan.filter { it.petani_id == currentPetaniId }
                } else {
                    allLahan
                }

                listLahan.clear()
                listLahan.addAll(userLahan)

                updateLahanDropdown(userLahan)

                val lahanMap = allLahan.associate { it.lahan_id to it.lahan_nama }

                val localItems = mutableListOf<KegiatanAdapter.ItemWithLahan>()
                localKegiatanFiltered.reversed().forEach { kegiatan ->
                    val detailLahan = database.DetailKegiatanDao().gethByKegiatan(kegiatan.localId)
                    val namaLahanList = detailLahan.mapNotNull { lahanMap[it.lahanId] }
                    val lahanStr = if (namaLahanList.isNotEmpty()) namaLahanList.joinToString(", ") else "-"

                    localItems.add(
                        KegiatanAdapter.ItemWithLahan(
                            kegiatan = kegiatan,
                            lahanNames = lahanStr
                        )
                    )
                }

                // 2. Cek koneksi internet
                if (!isOnline()) {
                    Toast.makeText(this@RiwayatKegiatanActivity, "Anda sedang offline. Menampilkan data lokal.", Toast.LENGTH_SHORT).show()
                    allItemsWithLahan.clear()
                    allItemsWithLahan.addAll(localItems)
                    sortAndApplyFilters()
                    binding.swipeRefresh.isRefreshing = false
                    return@launch
                }

                // 3. Jika online, ambil data dari API server
                PetaniApi.getRiwayatKegiatan(
                    petaniId = currentPetaniId,
                    callback = object : Callback {
                        override fun onFailure(call: Call, e: IOException) {
                            runOnUiThread {
                                binding.swipeRefresh.isRefreshing = false
                                Toast.makeText(this@RiwayatKegiatanActivity, "Anda sedang offline / Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                                allItemsWithLahan.clear()
                                allItemsWithLahan.addAll(localItems)
                                sortAndApplyFilters()
                            }
                        }

                        override fun onResponse(call: Call, response: Response) {
                            val jsonString = response.body?.string()
                            Log.d("API_KEGIATAN", "CODE: ${response.code}, BODY: $jsonString")

                            val serverItems = mutableListOf<KegiatanAdapter.ItemWithLahan>()

                            if (response.isSuccessful && !jsonString.isNullOrEmpty()) {
                                try {
                                    val trimmed = jsonString.trim()
                                    val dataArray: JSONArray? = if (trimmed.startsWith("[")) {
                                        JSONArray(trimmed)
                                    } else {
                                        val jsonObj = JSONObject(trimmed)
                                        when {
                                            jsonObj.has("data") -> jsonObj.optJSONArray("data")
                                            jsonObj.has("kegiatan") -> jsonObj.optJSONArray("kegiatan")
                                            else -> null
                                        }
                                    }

                                    if (dataArray != null) {
                                        for (i in 0 until dataArray.length()) {
                                            val obj = dataArray.optJSONObject(i) ?: continue

                                            val kegId = obj.optInt("kegiatan_id", obj.optInt("id", i + 1))
                                            val tgl = obj.optString("kegiatan_tanggal", obj.optString("tanggal", "-"))
                                            val jumlah = obj.optDouble("kegiatan_jumlah", obj.optDouble("jumlah", 1.0)).toInt()
                                            val satuan = obj.optString("kegiatan_satuan", obj.optString("satuan", "Liter"))

                                            val namaKeg = when {
                                                obj.has("nama_kegiatan") && !obj.isNull("nama_kegiatan") && obj.optString("nama_kegiatan").isNotBlank() && obj.optString("nama_kegiatan") != "null" -> obj.optString("nama_kegiatan")
                                                obj.has("jenis") && !obj.isNull("jenis") -> obj.optJSONObject("jenis")?.optString("nama_jenis", "Kegiatan Kebun") ?: "Kegiatan Kebun"
                                                obj.has("jenis_kegiatan") && !obj.isNull("jenis_kegiatan") -> obj.optJSONObject("jenis_kegiatan")?.optString("nama_jenis_kegiatan", "Kegiatan Kebun") ?: "Kegiatan Kebun"
                                                else -> "Kegiatan Kebun"
                                            }

                                            val rawBahan = if (obj.has("nama_bahan") && !obj.isNull("nama_bahan") && obj.optString("nama_bahan").isNotBlank() && obj.optString("nama_bahan") != "null") {
                                                obj.optString("nama_bahan")
                                            } else {
                                                obj.optString("kegiatan_ket", "-")
                                            }
                                            val bahan = if (rawBahan == "null" || rawBahan.isBlank()) "-" else rawBahan

                                            val rawLimbah = obj.optString("jenis_limbah", "-")
                                            val jenisLimbah = if (rawLimbah == "null" || rawLimbah.isBlank()) "-" else rawLimbah
                                            val rawStatus = obj.optString("status_limbah", "Belum Disetor")
                                            val statusLimbah = if (rawStatus == "null" || rawStatus.isBlank()) "Belum Disetor" else rawStatus

                                            // Extract Lahan Name
                                            var lahanNames = "-"
                                            if (obj.has("lahan_nama") && !obj.isNull("lahan_nama") && obj.optString("lahan_nama") != "null") {
                                                lahanNames = obj.optString("lahan_nama")
                                            } else {
                                                val detailArray = obj.optJSONArray("detail_lahan") ?: obj.optJSONArray("detail_kegiatan")
                                                val names = mutableListOf<String>()
                                                if (detailArray != null) {
                                                    for (j in 0 until detailArray.length()) {
                                                        val dObj = detailArray.optJSONObject(j)
                                                        val lObj = dObj?.optJSONObject("lahan")
                                                        if (lObj != null && lObj.has("lahan_nama") && !lObj.isNull("lahan_nama")) {
                                                            names.add(lObj.optString("lahan_nama"))
                                                        }
                                                    }
                                                }
                                                if (names.isNotEmpty()) lahanNames = names.joinToString(", ")
                                            }

                                            val kegiatanEntity = KegiatanEntity(
                                                localId = kegId,
                                                kegiatan_tanggal = tgl,
                                                kegiatan_jumlah = jumlah,
                                                kegiatan_satuan = satuan,
                                                nama_kegiatan = namaKeg,
                                                petani_id = currentPetaniId,
                                                kegiatan_ket = bahan,
                                                nama_bahan = bahan,
                                                jenis_limbah = jenisLimbah,
                                                status_limbah = statusLimbah,
                                                isSynced = true
                                            )

                                            serverItems.add(
                                                KegiatanAdapter.ItemWithLahan(
                                                    kegiatan = kegiatanEntity,
                                                    lahanNames = lahanNames
                                                )
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("API_KEGIATAN_ERR", "Parse error", e)
                                }
                            }

                            runOnUiThread {
                                binding.swipeRefresh.isRefreshing = false
                                allItemsWithLahan.clear()
                                // Tampilkan data lokal yang belum tersinkron
                                allItemsWithLahan.addAll(localItems)
                                // Tampilkan data resmi server
                                allItemsWithLahan.addAll(serverItems)
                                sortAndApplyFilters()
                            }
                        }
                    }
                )

            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@RiwayatKegiatanActivity, "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sortAndApplyFilters() {
        allItemsWithLahan.sortWith(
            compareByDescending<KegiatanAdapter.ItemWithLahan> { it.kegiatan.kegiatan_tanggal }
                .thenByDescending { !it.kegiatan.isSynced }
                .thenByDescending { it.kegiatan.localId }
        )
        updateYearDropdown()

        // Fallback: jika dropdown lahan masih kosong (hanya "Semua Lahan"),
        // ambil nama lahan unik dari riwayat kegiatan milik user yang sedang tampil
        if (lahanListDropdown.size <= 1) {
            val distinctActivityLahan = allItemsWithLahan.flatMap { item ->
                item.lahanNames.split(",").map { it.trim() }
            }.filter { it.isNotEmpty() && it != "-" && it != "null" }.distinct()

            if (distinctActivityLahan.isNotEmpty()) {
                val currentSelection = binding.spLahan.selectedItem?.toString() ?: "Semua Lahan"
                lahanListDropdown.clear()
                lahanListDropdown.add("Semua Lahan")
                lahanListDropdown.addAll(distinctActivityLahan)
                lahanAdapter?.notifyDataSetChanged()
                val pos = lahanListDropdown.indexOf(currentSelection)
                if (pos >= 0) binding.spLahan.setSelection(pos)
            }
        }

        applyFilters()
    }

    private fun parseDateYearMonth(dateStr: String): Pair<Int?, Int?> {
        val s = dateStr.trim()
        if (s.isEmpty() || s == "-") return Pair(null, null)

        val m1 = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})""").find(s)
        if (m1 != null) {
            val y = m1.groupValues[1].toIntOrNull()
            val m = m1.groupValues[2].toIntOrNull()
            return Pair(y, m)
        }

        val m2 = Regex("""^(\d{1,2})[-/](\d{1,2})[-/](\d{4})""").find(s)
        if (m2 != null) {
            val y = m2.groupValues[3].toIntOrNull()
            val m = m2.groupValues[2].toIntOrNull()
            return Pair(y, m)
        }

        val m3 = Regex("""^(\d{1,2})\s+([A-Za-z]+)\s+(\d{4})""").find(s)
        if (m3 != null) {
            val y = m3.groupValues[3].toIntOrNull()
            val mStr = m3.groupValues[2].lowercase(Locale.getDefault())
            val m = when {
                mStr.startsWith("jan") -> 1
                mStr.startsWith("feb") -> 2
                mStr.startsWith("mar") -> 3
                mStr.startsWith("apr") -> 4
                mStr.startsWith("mei") || mStr.startsWith("may") -> 5
                mStr.startsWith("jun") -> 6
                mStr.startsWith("jul") -> 7
                mStr.startsWith("agu") || mStr.startsWith("aug") -> 8
                mStr.startsWith("sep") -> 9
                mStr.startsWith("okt") || mStr.startsWith("oct") -> 10
                mStr.startsWith("nov") -> 11
                mStr.startsWith("des") || mStr.startsWith("dec") -> 12
                else -> null
            }
            return Pair(y, m)
        }

        return Pair(null, null)
    }

    private fun applyFilters() {
        val query = binding.etSearch.text?.toString()?.trim()?.lowercase(Locale.getDefault()) ?: ""
        val selectedBulanPos = binding.spBulan.selectedItemPosition // 0 = Semua Bulan, 1..12 = Jan..Des
        val selectedTahun = binding.spTahun.selectedItem?.toString()?.trim() ?: "Semua Tahun"
        val selectedLahan = binding.spLahan.selectedItem?.toString()?.trim() ?: "Semua Lahan"

        filteredItems.clear()

        for (item in allItemsWithLahan) {
            val k = item.kegiatan

            // 1. Filter Pencarian Text
            val matchesQuery = query.isEmpty() ||
                    k.nama_kegiatan.lowercase(Locale.getDefault()).contains(query) ||
                    k.nama_bahan.lowercase(Locale.getDefault()).contains(query) ||
                    k.kegiatan_ket.lowercase(Locale.getDefault()).contains(query) ||
                    k.jenis_limbah.lowercase(Locale.getDefault()).contains(query) ||
                    item.lahanNames.lowercase(Locale.getDefault()).contains(query)

            // 2. Filter Lahan
            val matchesLahan = selectedLahan.isEmpty() ||
                    selectedLahan == "Semua Lahan" ||
                    item.lahanNames.contains(selectedLahan, ignoreCase = true)

            // 3. Filter Tanggal (Tahun dan Bulan)
            val (itemYear, itemMonth) = parseDateYearMonth(k.kegiatan_tanggal)

            val matchesTahun = if (selectedTahun.isEmpty() || selectedTahun == "Semua Tahun") {
                true
            } else {
                if (itemYear != null) {
                    itemYear.toString() == selectedTahun
                } else {
                    k.kegiatan_tanggal.contains(selectedTahun)
                }
            }

            val matchesBulan = if (selectedBulanPos <= 0) {
                true
            } else {
                if (itemMonth != null) {
                    itemMonth == selectedBulanPos
                } else {
                    val monthName = bulanList.getOrNull(selectedBulanPos)?.lowercase(Locale.getDefault()) ?: ""
                    val monthPad = String.format(Locale.US, "-%02d-", selectedBulanPos)
                    k.kegiatan_tanggal.lowercase(Locale.getDefault()).contains(monthName) ||
                            k.kegiatan_tanggal.contains(monthPad)
                }
            }

            if (matchesQuery && matchesLahan && matchesTahun && matchesBulan) {
                filteredItems.add(item)
            }
        }

        adapter.updateItems(filteredItems)

        val targetKegiatanId = intent.getIntExtra("kegiatan_id", 0)
        if (targetKegiatanId != 0) {
            val target = filteredItems.find { it.kegiatan.localId == targetKegiatanId }
                ?: allItemsWithLahan.find { it.kegiatan.localId == targetKegiatanId }
            if (target != null) {
                intent.removeExtra("kegiatan_id")
                showDetailDialog(target.kegiatan)
            }
        }

        binding.tvTotalCatatan.text = "${filteredItems.size} Kegiatan"
        if (filteredItems.isEmpty()) {
            binding.rvKegiatan.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.rvKegiatan.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

        private fun showDetailDialog(item: KegiatanEntity) {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_detail_kegiatan, null)

        val tvDetailJenisKegiatan = view.findViewById<TextView>(R.id.tvDetailJenisKegiatan)
        val tvDetailTanggal = view.findViewById<TextView>(R.id.tvDetailTanggal)
        val tvDetailBahan = view.findViewById<TextView>(R.id.tvDetailBahan)
        val tvDetailDosis = view.findViewById<TextView>(R.id.tvDetailDosis)
        val tvDetailLimbah = view.findViewById<TextView>(R.id.tvDetailLimbah)
        val tvDetailStatusLimbahBadge = view.findViewById<TextView>(R.id.tvDetailStatusLimbahBadge)
        val tvDetailStatusSync = view.findViewById<TextView>(R.id.tvDetailStatusSync)
        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val btnTutup = view.findViewById<View>(R.id.btnTutup)

        val namaKeg = item.nama_kegiatan.ifEmpty {
            when (item.kegiatan_jenis) {
                1 -> "Pemupukan"
                2 -> "Pengendalian Gulma"
                3 -> "Pengendalian Hama"
                else -> "Kegiatan Kebun"
            }
        }
        val bahan = item.nama_bahan.ifEmpty { item.kegiatan_ket.ifEmpty { "-" } }
        val dosis = "${item.kegiatan_jumlah} ${item.kegiatan_satuan}"

        tvDetailJenisKegiatan.text = namaKeg
        tvDetailTanggal.text = item.kegiatan_tanggal.ifEmpty { "-" }
        tvDetailBahan.text = bahan
        tvDetailDosis.text = dosis
        tvDetailLimbah.text = item.jenis_limbah.ifEmpty { "Tidak Ada Limbah" }

        // Status Limbah Badge
        val statusLimbah = item.status_limbah.ifEmpty { "Belum Disetor" }
        tvDetailStatusLimbahBadge.text = statusLimbah
        if (statusLimbah.equals("Sudah Disetor", ignoreCase = true)) {
            tvDetailStatusLimbahBadge.setBackgroundResource(R.drawable.bg_badge_green)
            tvDetailStatusLimbahBadge.setTextColor(Color.parseColor("#1B4D2E"))
        } else if (statusLimbah.equals("Tidak Ada", ignoreCase = true) || item.jenis_limbah.isEmpty()) {
            tvDetailStatusLimbahBadge.setBackgroundResource(R.drawable.bg_badge_gold)
            tvDetailStatusLimbahBadge.setTextColor(Color.parseColor("#475569"))
        } else {
            tvDetailStatusLimbahBadge.setBackgroundResource(R.drawable.bg_badge_red)
            tvDetailStatusLimbahBadge.setTextColor(Color.parseColor("#B71C1C"))
        }

        // Status Sync Data
        if (item.isSynced) {
            tvDetailStatusSync.text = "Tersinkronisasi"
            tvDetailStatusSync.setTextColor(Color.parseColor("#1B4D2E"))
        } else {
            tvDetailStatusSync.text = "Mengantre Sync"
            tvDetailStatusSync.setTextColor(Color.parseColor("#B71C1C"))
        }

        val btnUbahStatusLimbah = view.findViewById<TextView>(R.id.btnUbahStatusLimbah)

        val onStatusChangeClick = View.OnClickListener {
            val options = arrayOf("Belum Disetor", "Sudah Disetor", "Tidak Ada")
            AlertDialog.Builder(this)
                .setTitle("Pilih Status Setor Limbah")
                .setItems(options) { _, which ->
                    val newStatus = options[which]
                    lifecycleScope.launch(Dispatchers.IO) {
                        val targetId = item.localId
                        if (targetId > 0) {
                            database.KegiatanDao().updateStatusLimbah(targetId, newStatus)
                        } else {
                            val newEntity = item.copy(status_limbah = newStatus, isSynced = false)
                            database.KegiatanDao().insert(newEntity)
                        }

                        withContext(Dispatchers.Main) {
                            tvDetailStatusLimbahBadge.text = newStatus
                            if (newStatus.equals("Sudah Disetor", ignoreCase = true)) {
                                tvDetailStatusLimbahBadge.setBackgroundResource(R.drawable.bg_badge_green)
                                tvDetailStatusLimbahBadge.setTextColor(Color.parseColor("#1B4D2E"))
                            } else if (newStatus.equals("Tidak Ada", ignoreCase = true)) {
                                tvDetailStatusLimbahBadge.setBackgroundResource(R.drawable.bg_rounded_gray)
                                tvDetailStatusLimbahBadge.setTextColor(Color.parseColor("#475569"))
                            } else {
                                tvDetailStatusLimbahBadge.setBackgroundResource(R.drawable.bg_badge_red)
                                tvDetailStatusLimbahBadge.setTextColor(Color.parseColor("#B71C1C"))
                            }

                            Toast.makeText(this@RiwayatKegiatanActivity, "Status setor limbah diperbarui ke: $newStatus", Toast.LENGTH_SHORT).show()
                            loadData()
                        }
                    }
                }
                .show()
        }

        btnUbahStatusLimbah?.setOnClickListener(onStatusChangeClick)
        tvDetailStatusLimbahBadge.setOnClickListener(onStatusChangeClick)

        btnClose.setOnClickListener { dialog.dismiss() }
        btnTutup.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(view)
        dialog.window?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)

        dialog.show()
    }



    private fun triggerKegiatanSync() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                com.example.notasawit.Sync.SyncKegiatanRepository(applicationContext, database).sync()
            } catch (e: Exception) {
                Log.e("SYNC_KEGIATAN", "Direct sync failed in RiwayatKegiatan", e)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
        com.example.notasawit.Utils.CustomAlert.checkAndShowPendingAlert(this)
    }
}