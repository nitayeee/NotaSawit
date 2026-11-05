package com.example.notasawit.Admin.Petani

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notasawit.Admin.PetaLahan.PetaLahanActivity
import com.example.notasawit.Network.ApiClient
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.Petani.PetaniEntity
import com.example.notasawit.Utils.AvatarHelper
import com.example.notasawit.databinding.ActivityDaftarPetaniAdminBinding
import com.example.notasawit.databinding.BottomSheetDetailPetaniBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class DaftarPetaniAdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDaftarPetaniAdminBinding
    private lateinit var adapter: PetaniAdminAdapter

    private val allPetaniList = mutableListOf<PetaniAdminItem>()
    private var adminDesaId: Int = 0
    private var adminDesaName: String = ""

    // Filter mode: true = Desa Saya saja, false = Semua Desa
    private var isFilterDesaSaya: Boolean = true
    private var currentSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDaftarPetaniAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        adminDesaId = sharedPref.getInt("admin_desa_id", 0)

        setupUI()
        setupRecyclerView()
        setupListeners()
        loadData()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener { finish() }

        // Atur status awal tab filter (Desa Saya aktif)
        updateFilterTabsUI()
    }

    private fun setupRecyclerView() {
        adapter = PetaniAdminAdapter { petani ->
            showDetailBottomSheet(petani)
        }
        binding.rvPetani.layoutManager = LinearLayoutManager(this)
        binding.rvPetani.adapter = adapter
    }

    private fun setupListeners() {
        // Tab Filter: Desa Saya
        binding.btnFilterDesaSaya.setOnClickListener {
            if (!isFilterDesaSaya) {
                isFilterDesaSaya = true
                updateFilterTabsUI()
                applyFilterAndSearch()
            }
        }

        // Tab Filter: Semua Desa
        binding.btnFilterSemuaDesa.setOnClickListener {
            if (isFilterDesaSaya) {
                isFilterDesaSaya = false
                updateFilterTabsUI()
                applyFilterAndSearch()
            }
        }

        // Search Input
        binding.etSearchPetani.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                currentSearchQuery = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (currentSearchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilterAndSearch()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Clear Search
        binding.btnClearSearch.setOnClickListener {
            binding.etSearchPetani.setText("")
            binding.btnClearSearch.visibility = View.GONE
            hideKeyboard()
        }

        // Swipe Refresh
        binding.swipeRefresh.setOnRefreshListener {
            loadData(isSwipeRefresh = true)
        }

        // Reset Filter dari Empty State
        binding.btnResetFilter.setOnClickListener {
            binding.etSearchPetani.setText("")
            isFilterDesaSaya = false
            updateFilterTabsUI()
            applyFilterAndSearch()
        }
    }

    private fun updateFilterTabsUI() {
        if (isFilterDesaSaya) {
            binding.btnFilterDesaSaya.setBackgroundResource(R.drawable.bg_filter_chip_active)
            binding.tvFilterDesaSaya.setTextColor(ContextCompat.getColor(this, R.color.primary))
            binding.tvFilterDesaSaya.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.poppins_bold)

            binding.btnFilterSemuaDesa.setBackgroundResource(R.drawable.bg_filter_chip_inactive)
            binding.tvFilterSemuaDesa.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.tvFilterSemuaDesa.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.poppins_medium)
        } else {
            binding.btnFilterDesaSaya.setBackgroundResource(R.drawable.bg_filter_chip_inactive)
            binding.tvFilterDesaSaya.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.tvFilterDesaSaya.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.poppins_medium)

            binding.btnFilterSemuaDesa.setBackgroundResource(R.drawable.bg_filter_chip_active)
            binding.tvFilterSemuaDesa.setTextColor(ContextCompat.getColor(this, R.color.primary))
            binding.tvFilterSemuaDesa.typeface = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.poppins_bold)
        }
    }

    private fun loadData(isSwipeRefresh: Boolean = false) {
        if (!isSwipeRefresh) {
            binding.progressBar.visibility = View.VISIBLE
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(this@DaftarPetaniAdminActivity)
            val masterDao = db.masterDao()

            // 1. Ambil nama desa admin dari Room atau daftar desa
            try {
                val desas = masterDao.getAllDesa()
                val foundDesa = desas.find { it.idDesa == adminDesaId }
                if (foundDesa != null) {
                    adminDesaName = foundDesa.namaDesa
                }
            } catch (e: Exception) {
                Log.e("DAFTAR_PETANI", "Error reading desa: ${e.message}")
            }

            // 2. Ambil cache awal dari Room
            val cachedPetani = try {
                masterDao.getAllPetani().map {
                    PetaniAdminItem(
                        idPetani = it.idPetani,
                        namaPetani = it.namaPetani,
                        username = "",
                        desaId = it.desaId,
                        namaDesa = it.namaDesa,
                        fotoProfil = it.fotoProfil
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }

            if (cachedPetani.isNotEmpty() && allPetaniList.isEmpty()) {
                withContext(Dispatchers.Main) {
                    allPetaniList.clear()
                    allPetaniList.addAll(cachedPetani)
                    applyFilterAndSearch()
                }
            }

            // 3. Ambil data segar dari API /api/petani
            val freshPetaniList = mutableListOf<PetaniAdminItem>()
            val roomEntitiesToSave = mutableListOf<PetaniEntity>()

            try {
                val request = Request.Builder()
                    .url("https://notasawit.pocari.id/api/petani")
                    .get()
                    .header("Accept", "application/json")
                    .build()

                val response = ApiClient.client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful && !body.isNullOrEmpty()) {
                    val json = JSONObject(body)
                    val dataArray = json.optJSONArray("data")
                    if (dataArray != null) {
                        for (i in 0 until dataArray.length()) {
                            val pObj = dataArray.getJSONObject(i)
                            val id = pObj.optInt("petani_id", -1)
                            val nama = pObj.optString("petani_nama", "")
                            val username = pObj.optString("petani_username", "")
                            val desaId = pObj.optInt("desa_id", 0)
                            val rawEmail = if (pObj.isNull("petani_email")) null else pObj.optString("petani_email", "")
                            val email = if (rawEmail == "null" || rawEmail.isNullOrBlank()) null else rawEmail

                            var desaNama = ""
                            if (pObj.has("desa") && !pObj.isNull("desa")) {
                                val dObj = pObj.optJSONObject("desa")
                                desaNama = dObj?.optString("desa_nama", "") ?: ""
                            }

                            if (desaId == adminDesaId && desaNama.isNotEmpty() && adminDesaName.isEmpty()) {
                                adminDesaName = desaNama
                            }

                            if (id != -1 && nama.isNotEmpty()) {
                                freshPetaniList.add(
                                    PetaniAdminItem(
                                        idPetani = id,
                                        namaPetani = nama,
                                        username = username,
                                        desaId = desaId,
                                        namaDesa = desaNama,
                                        email = if (email == "null" || email.isNullOrEmpty()) null else email
                                    )
                                )

                                roomEntitiesToSave.add(
                                    PetaniEntity(
                                        idPetani = id,
                                        namaPetani = nama,
                                        namaDesa = desaNama,
                                        desaId = desaId
                                    )
                                )
                            }
                        }
                    }

                    // Simpan ke Room Database
                    if (roomEntitiesToSave.isNotEmpty()) {
                        masterDao.insertPetani(roomEntitiesToSave)
                    }
                }
            } catch (e: Exception) {
                Log.e("DAFTAR_PETANI", "Error fetching fresh petani: ${e.message}")
            }

            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.GONE
                binding.swipeRefresh.isRefreshing = false

                if (freshPetaniList.isNotEmpty()) {
                    allPetaniList.clear()
                    allPetaniList.addAll(freshPetaniList)
                }

                // Update teks nama desa pada toolbar dan tab filter
                if (adminDesaName.isNotEmpty()) {
                    binding.tvToolbarSubtitle.text = "Desa $adminDesaName"
                    binding.tvFilterDesaSaya.text = "Desa $adminDesaName"
                } else if (adminDesaId > 0) {
                    binding.tvToolbarSubtitle.text = "Desa ID: $adminDesaId"
                    binding.tvFilterDesaSaya.text = "Desa Saya"
                } else {
                    binding.tvToolbarSubtitle.text = "Semua Wilayah"
                    binding.tvFilterDesaSaya.text = "Desa Saya"
                    isFilterDesaSaya = false
                    updateFilterTabsUI()
                }

                applyFilterAndSearch()
            }
        }
    }

    private fun applyFilterAndSearch() {
        val query = currentSearchQuery.lowercase().trim()

        val filtered = allPetaniList.filter { petani ->
            // Filter 1: Berdasarkan Wilayah Desa
            val matchesDesa = if (isFilterDesaSaya && adminDesaId > 0) {
                petani.desaId == adminDesaId
            } else {
                true // Tampilkan Semua Desa
            }

            // Filter 2: Berdasarkan Kata Kunci Pencarian
            val matchesSearch = if (query.isEmpty()) {
                true
            } else {
                petani.namaPetani.lowercase().contains(query) ||
                        petani.username.lowercase().contains(query) ||
                        petani.namaDesa.lowercase().contains(query) ||
                        (petani.email?.lowercase()?.contains(query) == true)
            }

            matchesDesa && matchesSearch
        }

        adapter.submitList(filtered)

        // Atur Tampilan Counter
        val totalCount = filtered.size
        val desaLabel = if (adminDesaName.isNotEmpty()) "Desa $adminDesaName" else "Desa Saya"
        if (isFilterDesaSaya && adminDesaId > 0) {
            binding.tvCountPetani.text = if (query.isEmpty()) {
                "Menampilkan $totalCount petani di $desaLabel"
            } else {
                "Ditemukan $totalCount petani di $desaLabel untuk \"$currentSearchQuery\""
            }
        } else {
            binding.tvCountPetani.text = if (query.isEmpty()) {
                "Menampilkan semua $totalCount petani terdaftar"
            } else {
                "Ditemukan $totalCount petani untuk \"$currentSearchQuery\""
            }
        }

        // Atur Empty State
        if (filtered.isEmpty()) {
            binding.rvPetani.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
            if (isFilterDesaSaya && adminDesaId > 0) {
                binding.tvEmptyTitle.text = "Belum Ada Petani di $desaLabel"
                binding.tvEmptySubtitle.text = "Tidak ditemukan petani di wilayah ini. Anda dapat melihat petani dari semua desa."
            } else {
                binding.tvEmptyTitle.text = "Petani Tidak Ditemukan"
                binding.tvEmptySubtitle.text = "Tidak ada petani yang cocok dengan pencarian \"$currentSearchQuery\"."
            }
        } else {
            binding.rvPetani.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun showDetailBottomSheet(petani: PetaniAdminItem) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetDetailPetaniBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        // Bind data awal yang sudah ada
        sheetBinding.tvSheetNama.text = petani.namaPetani
        sheetBinding.tvSheetUsername.text = if (petani.username.isNotEmpty()) "@${petani.username}" else "-"
        sheetBinding.tvSheetDesa.text = if (petani.namaDesa.isNotEmpty()) "Desa ${petani.namaDesa}" else "Desa ID: ${petani.desaId}"
        sheetBinding.tvSheetStatus.text = petani.status
        sheetBinding.tvSheetId.text = "#${petani.idPetani}"
        sheetBinding.tvSheetEmail.text = petani.email ?: "-"

        AvatarHelper.setupAvatar(
            imageView = sheetBinding.ivSheetProfil,
            textViewInitial = sheetBinding.tvSheetInitial,
            nama = petani.namaPetani,
            fotoPathOrUrl = petani.fotoProfil
        )

        var phoneNumber = petani.noHp

        // Ambil detail lengkap (nomor HP & alamat) dari API secara background
        PetaniApi.getDetailPetani(petani.idPetani, object : Callback {
            override fun onFailure(call: Call, e: IOException) {}

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                if (response.isSuccessful && !json.isNullOrEmpty()) {
                    try {
                        val respObj = JSONObject(json)
                        val dObj = respObj.optJSONObject("data")
                        if (dObj != null) {
                            val noHp = dObj.optString("petani_no_hp", "")
                            val alamat = dObj.optString("petani_alamat", "")
                            val status = dObj.optString("petani_status", "Aktif")
                            val profil = dObj.optString("petani_profil", "")

                            phoneNumber = noHp

                            runOnUiThread {
                                if (noHp.isNotEmpty() && noHp != "null") {
                                    sheetBinding.tvSheetNoHp.text = noHp
                                }
                                if (alamat.isNotEmpty() && alamat != "null") {
                                    sheetBinding.tvSheetAlamat.text = alamat
                                }
                                if (status.isNotEmpty() && status != "null") {
                                    sheetBinding.tvSheetStatus.text = status
                                }
                                if (profil.isNotEmpty() && profil != "null") {
                                    AvatarHelper.setupAvatar(
                                        imageView = sheetBinding.ivSheetProfil,
                                        textViewInitial = sheetBinding.tvSheetInitial,
                                        nama = petani.namaPetani,
                                        fotoPathOrUrl = profil
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

        // Tombol WhatsApp
        sheetBinding.btnSheetWa.setOnClickListener {
            val phone = phoneNumber?.trim() ?: ""
            if (phone.isEmpty() || phone == "null") {
                Toast.makeText(this, "Nomor telepon petani belum terdaftar", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var cleanPhone = phone.replace(Regex("[^0-9]"), "")
            if (cleanPhone.startsWith("0")) {
                cleanPhone = "62" + cleanPhone.substring(1)
            } else if (!cleanPhone.startsWith("62")) {
                cleanPhone = "62$cleanPhone"
            }

            val pesan = "Halo Bapak/Ibu ${petani.namaPetani}, saya Admin SILAUSA Desa ${petani.namaDesa}."
            val encodedMessage = Uri.encode(pesan)
            val waUri = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUri))
            try {
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }

        // Tombol Peta Lahan
        sheetBinding.btnSheetPeta.setOnClickListener {
            dialog.dismiss()
            val mapIntent = Intent(this, PetaLahanActivity::class.java)
            startActivity(mapIntent)
        }

        // Tombol Tutup
        sheetBinding.btnSheetTutup.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let { view ->
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}

