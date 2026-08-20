package com.example.notasawit.Admin.PetaLahan

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.ActivityPetaLahanBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class PetaLahanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetaLahanBinding
    private val allPolygonsList = mutableListOf<JSONObject>()
    private val petaniList = mutableListOf<String>()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetaLahanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Setup WebView
        binding.webViewPeta.settings.javaScriptEnabled = true
        binding.webViewPeta.settings.domStorageEnabled = true
        binding.webViewPeta.webChromeClient = WebChromeClient()
        binding.webViewPeta.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                fetchLahanPolygons()
            }
        }

        binding.webViewPeta.addJavascriptInterface(WebAppInterface(), "Android")

        // Load leaflet_map.html dari assets
        binding.webViewPeta.loadUrl("file:///android_asset/leaflet_map.html")
    }

    private fun fetchLahanPolygons() {
        binding.progressBar.visibility = View.VISIBLE
        
        // Kita panggil endpoint /lahan untuk mendapat semua lahan
        // Asumsi backend mengembalikan array lahan beserta kolom polygon/koordinat
        val request = okhttp3.Request.Builder()
            .url("http://160.187.144.157/api/lahan")
            .get()
            .build()
            
        com.example.notasawit.Network.ApiClient.client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this@PetaLahanActivity, "Gagal mengambil data peta", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                
                lifecycleScope.launch(Dispatchers.IO) {
                    val sharedPref = getSharedPreferences("NOTASAWIT_PREF", android.content.Context.MODE_PRIVATE)
                    val adminDesaId = sharedPref.getInt("admin_desa_id", 0)
                    val allPetaniList = AppDatabase.getDatabase(this@PetaLahanActivity).masterDao().getAllPetani()
                    val petaniListFromRoom = if (adminDesaId != 0) {
                        AppDatabase.getDatabase(this@PetaLahanActivity).masterDao().getPetaniByDesa(adminDesaId)
                    } else {
                        allPetaniList
                    }
                    val petaniMap = petaniListFromRoom.associate { it.idPetani to it.namaPetani }
                    val allPetaniMap = allPetaniList.associate { it.idPetani to it.namaPetani }

                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        if (response.isSuccessful && body != null) {
                            try {
                                val jsonObject = JSONObject(body)
                                val dataArray = jsonObject.getJSONArray("data")
                                val uniquePetani = mutableSetOf<String>()

                                allPolygonsList.clear()
                                petaniList.clear()
                                petaniList.add("Semua Petani")

                                if (dataArray.length() > 0) {
                                    Log.d("PETA", "Contoh data JSON item pertama: " + dataArray.getJSONObject(0).toString())
                                }

                                for (i in 0 until dataArray.length()) {
                                    val item = dataArray.getJSONObject(i)
                                    
                                    var polyStr: String? = null
                                    if (item.has("polygon") && !item.isNull("polygon")) {
                                        polyStr = item.getString("polygon")
                                    } else if (item.has("geo_json") && !item.isNull("geo_json")) {
                                        polyStr = item.getString("geo_json")
                                    } else if (item.has("lahan_polygon") && !item.isNull("lahan_polygon")) {
                                        polyStr = item.getString("lahan_polygon")
                                    } else if (item.has("koordinat") && !item.isNull("koordinat")) {
                                        polyStr = item.getString("koordinat")
                                    } else if (item.has("area_lahan") && !item.isNull("area_lahan")) {
                                        polyStr = item.getString("area_lahan")
                                    }

                                    if (polyStr != null) {
                                        try {
                                            val polyJson = JSONObject(polyStr)
                                            val properties = JSONObject()
                                            properties.put("lahan_nama", item.optString("lahan_nama", "Lahan"))
                                            
                                            // Ambil ID petani dan cari di Room / JSON
                                            val petaniId = if (item.has("petani_id")) item.optInt("petani_id") else item.optInt("id_petani", -1)
                                            var petaniNama = item.optString("petani_nama", item.optString("nama_petani", ""))
                                            if (petaniNama.isEmpty() || petaniNama == "null") {
                                                if (item.has("petani") && !item.isNull("petani")) {
                                                    val pObj = item.optJSONObject("petani")
                                                    petaniNama = pObj?.optString("petani_nama", pObj.optString("nama_petani", "")) ?: ""
                                                }
                                            }
                                            if (petaniNama.isEmpty() || petaniNama == "null") {
                                                petaniNama = petaniMap[petaniId] ?: allPetaniMap[petaniId] ?: "Unknown (ID: $petaniId)"
                                            }
                                            properties.put("petani_nama", petaniNama)
                                            
                                            polyJson.put("properties", properties)
                                            allPolygonsList.add(polyJson)
                                            
                                            if (!petaniNama.startsWith("Unknown")) {
                                                uniquePetani.add(petaniNama)
                                            } else {
                                                Log.d("PETA", "Petani ID $petaniId tidak ditemukan di Room")
                                            }
                                        } catch (e: Exception) {
                                            Log.e("PETA", "Gagal parse polygon string: " + e.message)
                                        }
                                    }
                                }

                                petaniList.addAll(uniquePetani.toList().sorted())

                                // Setup Spinner
                                val adapter = ArrayAdapter(this@PetaLahanActivity, android.R.layout.simple_spinner_item, petaniList)
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                binding.spinnerPetani.adapter = adapter
                                
                                binding.spinnerPetani.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                                        val selectedPetani = petaniList[position]
                                        filterAndRenderPolygons(selectedPetani)
                                    }
                                    override fun onNothingSelected(parent: AdapterView<*>) {}
                                }

                                // Render semua polygon saat pertama kali
                                filterAndRenderPolygons("Semua Petani")

                            } catch (e: Exception) {
                                Log.e("PETA", "Error parsing response: " + e.message)
                            }
                        }
                    }
                }
            }
        })
    }

    private fun filterAndRenderPolygons(petani: String) {
        val filteredArray = JSONArray()
        for (poly in allPolygonsList) {
            if (petani == "Semua Petani") {
                filteredArray.put(poly)
            } else {
                val props = poly.optJSONObject("properties")
                val pNama = props?.optString("petani_nama")
                if (pNama == petani) {
                    filteredArray.put(poly)
                }
            }
        }
        val script = "javascript:renderPolygons(${filteredArray.toString()})"
        binding.webViewPeta.evaluateJavascript(script, null)
    }

    // Interface untuk menerima klik dari Javascript
    inner class WebAppInterface {
        @JavascriptInterface
        fun openGoogleMaps(lat: Double, lng: Double, lahanNama: String) {
            // Buka intent Google Maps
            val uri = "geo:$lat,$lng?q=$lat,$lng($lahanNama)"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback jika tidak ada Google Maps app
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng"))
                startActivity(browserIntent)
            }
        }
    }
}
