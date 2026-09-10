package com.example.notasawit.InputKegiatan

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.KegiatanPetani.KegiatanEntity
import com.example.notasawit.Room.Lahan.LahanEntity
import com.example.notasawit.databinding.ActivityRiwayatKegiatanBinding
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class RiwayatKegiatanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRiwayatKegiatanBinding
    private lateinit var database: AppDatabase
    private lateinit var adapter: KegiatanAdapter

    private val allItemsWithLahan = mutableListOf<KegiatanAdapter.ItemWithLahan>()
    private val filteredItems = mutableListOf<KegiatanAdapter.ItemWithLahan>()
    private val listLahan = mutableListOf<LahanEntity>()

    private val sharedPref by lazy {
        getSharedPreferences("NOTASAWIT_PREF", MODE_PRIVATE)
    }

    private val spPetaniId by lazy {
        val id = sharedPref.getInt("petani_id", 0)
        if (id != 0) id else sharedPref.getInt("user_id", 0)
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
        val bulanList = listOf("Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        val bulanAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, bulanList)
        binding.spBulan.setAdapter(bulanAdapter)
        binding.spBulan.setOnItemClickListener { _, _, _, _ -> applyFilters() }

        val tahunList = listOf("Semua Tahun", "2026", "2025", "2024")
        val tahunAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, tahunList)
        binding.spTahun.setAdapter(tahunAdapter)
        binding.spTahun.setOnItemClickListener { _, _, _, _ -> applyFilters() }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    private fun loadData() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val currentPetaniId = spPetaniId

                // 1. Load data lokal (yang belum tersinkron) dari Room DB
                val rawLocalKegiatan = database.KegiatanDao().getAll()
                val localKegiatanFiltered = if (currentPetaniId != 0) {
                    val list = rawLocalKegiatan.filter { it.petani_id == currentPetaniId }
                    if (list.isNotEmpty()) list else rawLocalKegiatan
                } else {
                    rawLocalKegiatan
                }

                val allLahan = database.LahanDao().getAllLahan()
                listLahan.clear()
                listLahan.addAll(allLahan)

                val lahanNamesDropdown = mutableListOf("Semua Lahan")
                lahanNamesDropdown.addAll(allLahan.map { it.lahan_nama })
                val lahanAdapter = ArrayAdapter(this@RiwayatKegiatanActivity, android.R.layout.simple_dropdown_item_1line, lahanNamesDropdown)
                binding.spLahan.setAdapter(lahanAdapter)
                binding.spLahan.setOnItemClickListener { _, _, _, _ -> applyFilters() }

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
                                applyFilters()
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
        applyFilters()
    }

    private fun applyFilters() {
        val query = binding.etSearch.text.toString().trim().lowercase(Locale.getDefault())
        val selectedBulan = binding.spBulan.text.toString().trim()
        val selectedTahun = binding.spTahun.text.toString().trim()
        val selectedLahan = binding.spLahan.text.toString().trim()

        filteredItems.clear()

        for (item in allItemsWithLahan) {
            val k = item.kegiatan

            val matchesQuery = query.isEmpty() ||
                    k.nama_kegiatan.lowercase(Locale.getDefault()).contains(query) ||
                    k.nama_bahan.lowercase(Locale.getDefault()).contains(query) ||
                    k.kegiatan_ket.lowercase(Locale.getDefault()).contains(query) ||
                    k.jenis_limbah.lowercase(Locale.getDefault()).contains(query) ||
                    item.lahanNames.lowercase(Locale.getDefault()).contains(query)

            val matchesLahan = selectedLahan.isEmpty() ||
                    selectedLahan == "Semua Lahan" ||
                    item.lahanNames.contains(selectedLahan, ignoreCase = true)

            var matchesDate = true
            if (selectedTahun.isNotEmpty() && selectedTahun != "Semua Tahun") {
                if (!k.kegiatan_tanggal.contains(selectedTahun)) {
                    matchesDate = false
                }
            }

            if (matchesQuery && matchesLahan && matchesDate) {
                filteredItems.add(item)
            }
        }

        adapter.updateItems(filteredItems)

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
        val namaKeg = item.nama_kegiatan.ifEmpty { "Kegiatan Kebun" }
        val bahan = item.nama_bahan.ifEmpty { item.kegiatan_ket.ifEmpty { "-" } }
        val dosis = "${item.kegiatan_jumlah} ${item.kegiatan_satuan}"
        val statusSync = if (item.isSynced) "Tersinkronisasi dari Server" else "Belum Tersinkron (Tersimpan Lokal)"

        val message = """
            📌 Jenis Kegiatan: $namaKeg
            📅 Tanggal Rencana: ${item.kegiatan_tanggal}
            🧪 Bahan / Pupuk / Racun: $bahan
            ⚖️ Dosis Penggunaan: $dosis
            🗑️ Jenis Limbah: ${item.jenis_limbah.ifEmpty { "-" }}
            📦 Status Limbah: ${item.status_limbah}
            🔄 Status Data: $statusSync
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("Detail Record Kegiatan Kebun")
            .setMessage(message)
            .setPositiveButton("Tutup", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }
}