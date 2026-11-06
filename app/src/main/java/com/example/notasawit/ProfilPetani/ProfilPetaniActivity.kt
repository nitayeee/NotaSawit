package com.example.notasawit.ProfilPetani

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityProfilPetaniBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class ProfilPetaniActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfilPetaniBinding
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE) }
    private val petaniId by lazy { sharedPref.getInt("petani_id", 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        binding = ActivityProfilPetaniBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnEditProfil.setOnClickListener {
            val intent = android.content.Intent(this, EditProfilPetaniActivity::class.java)
            startActivity(intent)
        }

        binding.btnEditLahan.setOnClickListener {
            val intent = android.content.Intent(this, EditLahanActivity::class.java)
            startActivity(intent)
        }

        if (petaniId == 0) {
            com.example.notasawit.Utils.CustomAlert.showError(this, "Gagal", "Petani ID tidak ditemukan")
        }

        // Konfigurasi WebView untuk Leaflet Map
        binding.mapViewLahan.settings.javaScriptEnabled = true
        binding.mapViewLahan.settings.domStorageEnabled = true
        binding.mapViewLahan.settings.cacheMode = WebSettings.LOAD_DEFAULT
        
        // Memastikan peta bisa digeser tanpa menggeser ScrollView utama
        binding.mapViewLahan.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }
        
        binding.mapViewLahan.addJavascriptInterface(WebAppInterface(), "Android")
        binding.mapViewLahan.loadUrl("file:///android_asset/leaflet_map.html")
    }

    override fun onResume() {
        super.onResume()
        if (petaniId != 0) {
            fetchDataProfil(petaniId)
            fetchDataLahan(petaniId)
        }
    }

    private fun fetchDataProfil(id: Int) {
        PetaniApi.getDetailPetani(id, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    com.example.notasawit.Utils.CustomAlert.showError(this@ProfilPetaniActivity, "Gagal", "Gagal mengambil data: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = JSONObject(body)
                            val data = jsonObject.optJSONObject("data")
                            
                            if (data != null) {
                                val nama = data.optString("petani_nama", "-")
                                val username = data.optString("petani_username", "-")
                                val email = data.optString("petani_email", "-")
                                val jenisKelamin = data.optString("petani_jenis_kelamin", "-")
                                val profilUrl = data.optString("petani_profil", "")
                                
                                // Sync local SharedPreferences
                                with(sharedPref.edit()) {
                                    putString("namaPetani", nama)
                                    putString("profilPetani", profilUrl)
                                    apply()
                                }

                                runOnUiThread {
                                    binding.tvNamaProfile.text = nama
                                    binding.tvNama.text = nama
                                    binding.tvUsername.text = username
                                    binding.tvEmail.text = email
                                    binding.tvJenisKelamin.text = jenisKelamin
                                    // Set Initials
                                    val parts = nama.trim().split("\\s+".toRegex())
                                    val initials = if (parts.size >= 2) {
                                        "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
                                    } else if (parts.isNotEmpty() && parts[0].isNotEmpty()) {
                                        parts[0].take(2).uppercase()
                                    } else {
                                        "U"
                                    }
                                    binding.tvInitials.text = initials

                                    if (profilUrl.isNotEmpty() && profilUrl != "null") {
                                        binding.imgFotoProfil.visibility = android.view.View.VISIBLE
                                        val fullUrl = if (!profilUrl.startsWith("http")) {
                                            "https://notasawit.pocari.id/storage/$profilUrl"
                                        } else {
                                            profilUrl
                                        }
                                        Glide.with(this@ProfilPetaniActivity)
                                            .load(fullUrl)
                                            .into(binding.imgFotoProfil)
                                    } else {
                                        binding.imgFotoProfil.visibility = android.view.View.GONE
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ProfilPetani", "Error parsing JSON: ${e.message}")
                            runOnUiThread {
                                com.example.notasawit.Utils.CustomAlert.showError(this@ProfilPetaniActivity, "Gagal", "Format response tidak sesuai")
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ProfilPetaniActivity, "Gagal mendapatkan data", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun fetchDataLahan(id: Int) {
        PetaniApi.getLahanByPetani(id, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ProfilPetani", "Gagal load lahan", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = JSONObject(body)
                            val dataArray = jsonObject.optJSONArray("data")
                            val jumlahLahan = dataArray?.length() ?: 0
                            var totalLuas = 0.0
                            val geoJsonList = JSONArray()

                            if (dataArray != null) {
                                for (i in 0 until dataArray.length()) {
                                    val lahanObj = dataArray.optJSONObject(i) ?: continue
                                    val lahanId = lahanObj.optInt("lahan_id", -1)
                                    val lahanNama = lahanObj.optString("lahan_nama", "Lahan #${i + 1}")
                                    val lahanLokasi = lahanObj.optString("lahan_lokasi", "-")
                                    val luas = if (lahanObj.has("lahan_luas") && !lahanObj.isNull("lahan_luas")) {
                                        lahanObj.optDouble("lahan_luas", 0.0)
                                    } else if (lahanObj.has("luas_lahan") && !lahanObj.isNull("luas_lahan")) {
                                        lahanObj.optDouble("luas_lahan", 0.0)
                                    } else {
                                        0.0
                                    }
                                    totalLuas += luas

                                    val areaLahanRaw = lahanObj.opt("area_lahan")
                                    var polyJson: JSONObject? = null
                                    if (areaLahanRaw is JSONObject) {
                                        polyJson = JSONObject(areaLahanRaw.toString())
                                    } else if (areaLahanRaw is String && areaLahanRaw.isNotEmpty() && areaLahanRaw != "null") {
                                        try {
                                            polyJson = JSONObject(areaLahanRaw)
                                        } catch (e: Exception) {
                                            Log.e("ProfilPetani", "Gagal parse string area_lahan ke JSON: ${e.message}")
                                        }
                                    }

                                    if (polyJson != null) {
                                        val featureObj = if (polyJson.optString("type") == "Feature") {
                                            polyJson
                                        } else {
                                            JSONObject().apply {
                                                put("type", "Feature")
                                                put("geometry", polyJson)
                                            }
                                        }
                                        val props = featureObj.optJSONObject("properties") ?: JSONObject()
                                        props.put("lahan_id", lahanId)
                                        props.put("lahan_nama", lahanNama)
                                        props.put("lahan_lokasi", lahanLokasi)
                                        props.put("lahan_luas", if (luas > 0) String.format(Locale.US, "%.2f", luas) else "")
                                        props.put("petani_nama", sharedPref.getString("namaPetani", "-"))
                                        featureObj.put("properties", props)

                                        geoJsonList.put(featureObj)
                                    }
                                }
                            }

                            runOnUiThread {
                                binding.tvJumlahLahan.text = "$jumlahLahan"
                                binding.tvTotalLuas.text = if (totalLuas > 0) String.format(Locale.US, "%.2f", totalLuas) else "-"

                                if (geoJsonList.length() > 0) {
                                    val jsCommand = "renderPolygons(${geoJsonList.toString()});"
                                    // Panggil JS dengan sedikit delay agar memastikan webview dan leaflet sudah dimuat sepenuhnya
                                    binding.mapViewLahan.postDelayed({
                                        binding.mapViewLahan.evaluateJavascript(jsCommand, null)
                                    }, 1000)
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ProfilPetani", "Error parsing Lahan: ${e.message}")
                        }
                    }
                }
            }
        })
    }

    // Interface untuk aksi dari Popup Leaflet Map
    inner class WebAppInterface {
        @JavascriptInterface
        fun openGoogleMaps(lat: Double, lng: Double, lahanNama: String) {
            runOnUiThread {
                try {
                    val uri = "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(lahanNama)})"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
                    intent.setPackage("com.google.android.apps.maps")
                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                        )
                        startActivity(browserIntent)
                    }
                } catch (e: Exception) {
                    val browserIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                    )
                    startActivity(browserIntent)
                }
            }
        }

        @JavascriptInterface
        fun bukaKelolaLahan(lahanId: Int) {
            runOnUiThread {
                val intent = Intent(this@ProfilPetaniActivity, EditLahanActivity::class.java)
                intent.putExtra("SELECTED_LAHAN_ID", lahanId)
                startActivity(intent)
            }
        }
    }
}