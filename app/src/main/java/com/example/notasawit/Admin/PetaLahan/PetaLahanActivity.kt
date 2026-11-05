package com.example.notasawit.Admin.PetaLahan

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Network.ApiClient
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.ActivityPetaLahanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

class PetaLahanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetaLahanBinding
    private val allPolygonsList = mutableListOf<JSONObject>()
    private val petaniList = mutableListOf<String>()
    private var isMapReady = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPetaLahanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener { finish() }

        // Minta Izin Lokasi di Android jika belum diizinkan
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            androidx.core.app.ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                1001
            )
        }

        // Setup WebView Leaflet
        binding.webViewPeta.settings.javaScriptEnabled = true
        binding.webViewPeta.settings.domStorageEnabled = true
        binding.webViewPeta.settings.setGeolocationEnabled(true)

        binding.webViewPeta.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(
                origin: String?,
                callback: android.webkit.GeolocationPermissions.Callback?
            ) {
                callback?.invoke(origin, true, false)
            }
        }

        binding.webViewPeta.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapReady = true
                fetchLahanAndPetaniData()
            }
        }

        binding.webViewPeta.addJavascriptInterface(WebAppInterface(), "Android")

        // Load leaflet_map.html dari assets
        binding.webViewPeta.loadUrl("file:///android_asset/leaflet_map.html")
    }

    private fun fetchLahanAndPetaniData() {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvLahanCount.text = "Memuat data lahan & petani..."

        lifecycleScope.launch(Dispatchers.IO) {
            val petaniMap = mutableMapOf<Int, String>()

            // 1. Ambil data dari Room Database sebagai cache awal
            try {
                val masterDao = AppDatabase.getDatabase(this@PetaLahanActivity).masterDao()
                val roomPetani = masterDao.getAllPetani()
                for (p in roomPetani) {
                    petaniMap[p.idPetani] = p.namaPetani
                }
            } catch (e: Exception) {
                Log.e("PETA", "Error reading Room database: ${e.message}")
            }

            // 2. Ambil data Petani lengkap dari API agar nama petani selalu terisi
            try {
                val petaniRequest = Request.Builder()
                    .url("https://notasawit.pocari.id/api/petani")
                    .get()
                    .header("Accept", "application/json")
                    .build()

                val petaniResponse = ApiClient.client.newCall(petaniRequest).execute()
                val petaniBody = petaniResponse.body?.string()

                if (petaniResponse.isSuccessful && !petaniBody.isNullOrEmpty()) {
                    val jsonPetani = JSONObject(petaniBody)
                    val petaniArray = jsonPetani.optJSONArray("data")
                    if (petaniArray != null) {
                        for (i in 0 until petaniArray.length()) {
                            val p = petaniArray.getJSONObject(i)
                            val id = p.optInt("petani_id", -1)
                            val nama = p.optString("petani_nama", "")
                            if (id != -1 && nama.isNotEmpty()) {
                                petaniMap[id] = nama
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PETA", "Error fetching petani API: ${e.message}")
            }

            // 3. Ambil data Lahan dari API
            try {
                val lahanRequest = Request.Builder()
                    .url("https://notasawit.pocari.id/api/lahan")
                    .get()
                    .header("Accept", "application/json")
                    .build()

                val lahanResponse = ApiClient.client.newCall(lahanRequest).execute()
                val lahanBody = lahanResponse.body?.string()

                if (lahanResponse.isSuccessful && !lahanBody.isNullOrEmpty()) {
                    val jsonObject = JSONObject(lahanBody)
                    val dataArray = jsonObject.optJSONArray("data") ?: JSONArray()
                    val uniquePetani = mutableSetOf<String>()
                    val parsedPolygons = mutableListOf<JSONObject>()

                    for (i in 0 until dataArray.length()) {
                        val item = dataArray.getJSONObject(i)
                        val lahanId = item.optInt("lahan_id", -1)
                        val petaniId = if (item.has("petani_id")) item.optInt("petani_id") else item.optInt("id_petani", -1)
                        val lahanNama = item.optString("lahan_nama", "Lahan #$lahanId")
                        val lahanLuas = item.optString("lahan_luas", "")
                        val lahanLokasi = item.optString("lahan_lokasi", "")
                        val tahunTanam = item.optString("tahun_tanam", "-")
                        val noSurat = item.optString("lahan_no_surat", "-")

                        // Cari nama petani dari objek, map, atau fallback
                        var petaniNama = item.optString("petani_nama", item.optString("nama_petani", ""))
                        if (petaniNama.isEmpty() || petaniNama == "null") {
                            val pObj = item.optJSONObject("petani")
                            if (pObj != null) {
                                petaniNama = pObj.optString("petani_nama", pObj.optString("nama_petani", ""))
                            }
                        }
                        if (petaniNama.isEmpty() || petaniNama == "null") {
                            petaniNama = petaniMap[petaniId] ?: "Petani #$petaniId"
                        }

                        // Parse area_lahan / polygon (mendukung JSONObject maupun String)
                        val polyJson: JSONObject? = when {
                            item.has("area_lahan") && !item.isNull("area_lahan") -> {
                                val obj = item.optJSONObject("area_lahan")
                                if (obj != null) {
                                    JSONObject(obj.toString())
                                } else {
                                    val str = item.optString("area_lahan", "")
                                    if (str.isNotEmpty()) try { JSONObject(str) } catch (e: Exception) { null } else null
                                }
                            }
                            item.has("polygon") && !item.isNull("polygon") -> {
                                val obj = item.optJSONObject("polygon")
                                if (obj != null) {
                                    JSONObject(obj.toString())
                                } else {
                                    val str = item.optString("polygon", "")
                                    if (str.isNotEmpty()) try { JSONObject(str) } catch (e: Exception) { null } else null
                                }
                            }
                            item.has("geo_json") && !item.isNull("geo_json") -> {
                                val obj = item.optJSONObject("geo_json")
                                if (obj != null) {
                                    JSONObject(obj.toString())
                                } else {
                                    val str = item.optString("geo_json", "")
                                    if (str.isNotEmpty()) try { JSONObject(str) } catch (e: Exception) { null } else null
                                }
                            }
                            item.has("lahan_polygon") && !item.isNull("lahan_polygon") -> {
                                val obj = item.optJSONObject("lahan_polygon")
                                if (obj != null) {
                                    JSONObject(obj.toString())
                                } else {
                                    val str = item.optString("lahan_polygon", "")
                                    if (str.isNotEmpty()) try { JSONObject(str) } catch (e: Exception) { null } else null
                                }
                            }
                            item.has("koordinat") && !item.isNull("koordinat") -> {
                                val obj = item.optJSONObject("koordinat")
                                if (obj != null) {
                                    JSONObject(obj.toString())
                                } else {
                                    val str = item.optString("koordinat", "")
                                    if (str.isNotEmpty()) try { JSONObject(str) } catch (e: Exception) { null } else null
                                }
                            }
                            else -> null
                        }

                        if (polyJson != null) {
                            val properties = JSONObject().apply {
                                put("lahan_id", lahanId)
                                put("lahan_nama", lahanNama)
                                put("petani_id", petaniId)
                                put("petani_nama", petaniNama)
                                put("lahan_luas", lahanLuas)
                                put("lahan_lokasi", lahanLokasi)
                                put("tahun_tanam", tahunTanam)
                                put("lahan_no_surat", noSurat)
                            }
                            polyJson.put("properties", properties)
                            parsedPolygons.add(polyJson)

                            if (petaniNama.isNotEmpty() && !petaniNama.startsWith("Petani #")) {
                                uniquePetani.add(petaniNama)
                            } else {
                                uniquePetani.add(petaniNama)
                            }
                        } else {
                            Log.w("PETA", "Item lahan ID $lahanId tidak memiliki koordinat polygon valid")
                        }
                    }

                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        allPolygonsList.clear()
                        allPolygonsList.addAll(parsedPolygons)

                        petaniList.clear()
                        petaniList.add("Semua Petani")
                        petaniList.addAll(uniquePetani.toList().sorted())

                        setupSearchAutoComplete()
                        filterAndRenderPolygons("")
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        binding.tvLahanCount.text = "Gagal memuat data lahan dari server"
                        Toast.makeText(this@PetaLahanActivity, "Gagal memuat data lahan", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("PETA", "Error fetching lahan API: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.tvLahanCount.text = "Gagal terhubung ke server"
                    Toast.makeText(this@PetaLahanActivity, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSearchAutoComplete() {
        val adapter = ArrayAdapter(
            this,
            R.layout.item_dropdown_petani,
            petaniList
        )
        binding.etSearchPetani.setAdapter(adapter)
        binding.etSearchPetani.threshold = 1

        binding.etSearchPetani.setOnClickListener {
            binding.etSearchPetani.showDropDown()
        }

        binding.etSearchPetani.setOnItemClickListener { parent, _, position, _ ->
            val selected = parent.getItemAtPosition(position).toString()
            if (selected == "Semua Petani") {
                binding.etSearchPetani.setText("")
            } else {
                binding.etSearchPetani.setText(selected)
            }
            binding.etSearchPetani.setSelection(binding.etSearchPetani.text.length)
            filterAndRenderPolygons(binding.etSearchPetani.text.toString())
            hideKeyboard()
        }

        binding.etSearchPetani.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                filterAndRenderPolygons(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchPetani.setText("")
            binding.btnClearSearch.visibility = View.GONE
            filterAndRenderPolygons("")
            hideKeyboard()
        }
    }

    private fun filterAndRenderPolygons(query: String) {
        if (!isMapReady) return

        val cleanQuery = query.trim().lowercase()
        val filteredArray = JSONArray()

        for (poly in allPolygonsList) {
            val props = poly.optJSONObject("properties")
            val pNama = props?.optString("petani_nama", "") ?: ""
            val lNama = props?.optString("lahan_nama", "") ?: ""

            if (cleanQuery.isEmpty() || cleanQuery == "semua petani" || cleanQuery == "semua") {
                filteredArray.put(poly)
            } else {
                if (pNama.lowercase().contains(cleanQuery) || lNama.lowercase().contains(cleanQuery)) {
                    filteredArray.put(poly)
                }
            }
        }

        val count = filteredArray.length()
        val total = allPolygonsList.size
        if (cleanQuery.isEmpty() || cleanQuery == "semua petani") {
            binding.tvLahanCount.text = "Menampilkan semua $total petak lahan"
        } else {
            binding.tvLahanCount.text = "Ditemukan $count dari $total petak lahan untuk \"$query\""
        }

        val script = "javascript:renderPolygons(${filteredArray.toString()})"
        binding.webViewPeta.evaluateJavascript(script, null)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        currentFocus?.let { view ->
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    // Interface untuk menerima klik dari Javascript di Leaflet
    inner class WebAppInterface {
        @JavascriptInterface
        fun openGoogleMaps(lat: Double, lng: Double, lahanNama: String) {
            val uri = "geo:$lat,$lng?q=$lat,$lng($lahanNama)"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback jika Google Maps app belum terinstal
                val browserIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                )
                startActivity(browserIntent)
            }
        }
    }
}
