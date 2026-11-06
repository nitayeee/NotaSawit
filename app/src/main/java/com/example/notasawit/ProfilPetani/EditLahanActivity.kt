package com.example.notasawit.ProfilPetani

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.Utils.CustomAlert
import com.example.notasawit.databinding.ActivityEditLahanBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

class EditLahanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditLahanBinding
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE) }
    private val petaniId by lazy { sharedPref.getInt("petani_id", 0) }

    // List untuk menyimpan data lahan
    private val lahanList = mutableListOf<LahanData>()
    private var selectedLahanId: Int = -1
    private var preselectedLahanId: Int = -1
    private var isMapLoaded = false

    data class LahanData(
        val id: Int,
        val nama: String,
        val lokasi: String,
        val luas: Double,
        var tahunTanam: String,
        var noSurat: String,
        val areaLahan: JSONObject?,
        val centerLat: Double?,
        val centerLng: Double?
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditLahanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preselectedLahanId = intent.getIntExtra("SELECTED_LAHAN_ID", -1)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupWebViewMap()

        // Handle spinner selection
        binding.spinnerLahan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < lahanList.size) {
                    val selectedLahan = lahanList[position]
                    selectedLahanId = selectedLahan.id
                    displayLahanDetail(selectedLahan)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedLahanId = -1
            }
        }

        binding.btnBukaMaps.setOnClickListener {
            if (selectedLahanId == -1 || lahanList.isEmpty()) {
                CustomAlert.showError(this, "Info", "Silakan pilih lahan terlebih dahulu")
                return@setOnClickListener
            }
            val pos = binding.spinnerLahan.selectedItemPosition
            if (pos in lahanList.indices) {
                val selected = lahanList[pos]
                openGoogleMaps(selected.centerLat, selected.centerLng, selected.nama, selected.lokasi)
            }
        }

        binding.btnSimpan.setOnClickListener {
            if (selectedLahanId == -1) {
                CustomAlert.showError(this, "Gagal", "Silakan pilih lahan terlebih dahulu")
                return@setOnClickListener
            }

            val tahunTanam = binding.etTahunTanam.text.toString().trim()
            val noSurat = binding.etNoSurat.text.toString().trim()

            updateLahan(selectedLahanId, tahunTanam, noSurat)
        }

        // Fetch data lahan dari server
        fetchLahan()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebViewMap() {
        binding.mapViewDetailLahan.settings.javaScriptEnabled = true
        binding.mapViewDetailLahan.settings.domStorageEnabled = true
        binding.mapViewDetailLahan.settings.cacheMode = WebSettings.LOAD_DEFAULT

        binding.mapViewDetailLahan.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            false
        }

        binding.mapViewDetailLahan.addJavascriptInterface(WebAppInterface(), "Android")
        binding.mapViewDetailLahan.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapLoaded = true
                val pos = binding.spinnerLahan.selectedItemPosition
                if (pos in lahanList.indices) {
                    renderLahanPolygon(lahanList[pos])
                }
            }
        }

        binding.mapViewDetailLahan.loadUrl("file:///android_asset/leaflet_map.html")
    }

    private fun displayLahanDetail(lahan: LahanData) {
        // Tampilkan Detail Text
        binding.tvDetailNamaLahan.text = lahan.nama
        binding.tvDetailLuasLahan.text = if (lahan.luas > 0) String.format(Locale.US, "%.2f Ha", lahan.luas) else "- Ha"
        binding.tvDetailLokasi.text = if (lahan.lokasi.isNotEmpty() && lahan.lokasi != "null") lahan.lokasi else "-"
        binding.tvDetailTahunTanam.text = if (lahan.tahunTanam.isNotEmpty() && lahan.tahunTanam != "null") lahan.tahunTanam else "Belum diisi"
        binding.tvDetailNoSurat.text = if (lahan.noSurat.isNotEmpty() && lahan.noSurat != "null") lahan.noSurat else "Belum diisi"

        if (lahan.centerLat != null && lahan.centerLng != null) {
            binding.tvDetailKoordinat.text = String.format(Locale.US, "%.6f, %.6f", lahan.centerLat, lahan.centerLng)
            binding.cardMapContainer.visibility = View.VISIBLE
        } else {
            binding.tvDetailKoordinat.text = "Belum ada titik koordinat"
            binding.cardMapContainer.visibility = View.GONE
        }

        // Isi form edit
        binding.etTahunTanam.setText(if (lahan.tahunTanam != "null" && lahan.tahunTanam.isNotEmpty()) lahan.tahunTanam else "")
        binding.etNoSurat.setText(if (lahan.noSurat != "null" && lahan.noSurat.isNotEmpty()) lahan.noSurat else "")

        // Render polygon pada peta preview
        renderLahanPolygon(lahan)
    }

    private fun renderLahanPolygon(lahan: LahanData) {
        if (!isMapLoaded) return
        if (lahan.areaLahan != null) {
            val featureObj = if (lahan.areaLahan.optString("type") == "Feature") {
                lahan.areaLahan
            } else {
                JSONObject().apply {
                    put("type", "Feature")
                    put("geometry", lahan.areaLahan)
                }
            }
            val props = featureObj.optJSONObject("properties") ?: JSONObject()
            props.put("lahan_id", lahan.id)
            props.put("lahan_nama", lahan.nama)
            props.put("lahan_luas", if (lahan.luas > 0) String.format(Locale.US, "%.2f", lahan.luas) else "")
            props.put("lahan_lokasi", lahan.lokasi)
            featureObj.put("properties", props)

            val array = JSONArray().put(featureObj)
            val script = "renderPolygons(${array.toString()});"
            binding.mapViewDetailLahan.evaluateJavascript(script, null)
        }
    }

    private fun fetchLahan() {
        if (petaniId == 0) return

        PetaniApi.getLahanByPetani(petaniId, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    CustomAlert.showError(this@EditLahanActivity, "Gagal", "Tidak dapat memuat data lahan: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val jsonObject = JSONObject(body)
                            val dataArray = jsonObject.optJSONArray("data")

                            lahanList.clear()

                            if (dataArray != null) {
                                for (i in 0 until dataArray.length()) {
                                    val obj = dataArray.optJSONObject(i) ?: continue
                                    val luas = if (obj.has("lahan_luas") && !obj.isNull("lahan_luas")) {
                                        obj.optDouble("lahan_luas", 0.0)
                                    } else {
                                        obj.optDouble("luas_lahan", 0.0)
                                    }

                                    val areaRaw = obj.opt("area_lahan")
                                    var areaObj: JSONObject? = null
                                    if (areaRaw is JSONObject) {
                                        areaObj = areaRaw
                                    } else if (areaRaw is String && areaRaw.isNotEmpty() && areaRaw != "null") {
                                        try {
                                            areaObj = JSONObject(areaRaw)
                                        } catch (e: Exception) {
                                            Log.e("EditLahan", "Gagal parse area_lahan: ${e.message}")
                                        }
                                    }

                                    val center = calculateCentroid(areaObj)

                                    lahanList.add(
                                        LahanData(
                                            id = obj.optInt("lahan_id", -1),
                                            nama = obj.optString("lahan_nama", "Lahan #${i + 1}"),
                                            lokasi = obj.optString("lahan_lokasi", "-"),
                                            luas = luas,
                                            tahunTanam = obj.optString("tahun_tanam", ""),
                                            noSurat = obj.optString("lahan_no_surat", ""),
                                            areaLahan = areaObj,
                                            centerLat = center?.first,
                                            centerLng = center?.second
                                        )
                                    )
                                }
                            }

                            runOnUiThread {
                                if (lahanList.isEmpty()) {
                                    CustomAlert.showError(this@EditLahanActivity, "Info", "Anda belum memiliki data lahan.")
                                } else {
                                    setupSpinner()
                                }
                            }

                        } catch (e: Exception) {
                            Log.e("EditLahan", "Error parsing JSON", e)
                        }
                    }
                } else {
                    runOnUiThread {
                        CustomAlert.showError(this@EditLahanActivity, "Gagal", "Gagal mengambil data lahan dari server")
                    }
                }
            }
        })
    }

    private fun setupSpinner() {
        val namaLahanList = lahanList.map { "${it.nama} (${it.lokasi})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, namaLahanList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerLahan.adapter = adapter

        // Pilih preselected lahan jika ada
        var selectedIndex = 0
        if (preselectedLahanId != -1) {
            val idx = lahanList.indexOfFirst { it.id == preselectedLahanId }
            if (idx != -1) {
                selectedIndex = idx
            }
        }
        binding.spinnerLahan.setSelection(selectedIndex)
        if (lahanList.isNotEmpty()) {
            displayLahanDetail(lahanList[selectedIndex])
        }
    }

    private fun calculateCentroid(geoJson: JSONObject?): Pair<Double, Double>? {
        if (geoJson == null) return null
        try {
            val coords = when {
                geoJson.has("coordinates") -> geoJson.optJSONArray("coordinates")
                geoJson.has("geometry") -> geoJson.optJSONObject("geometry")?.optJSONArray("coordinates")
                else -> null
            } ?: return null

            var ring: JSONArray? = null
            if (coords.length() > 0) {
                val first = coords.opt(0)
                if (first is JSONArray && first.length() > 0) {
                    val sub = first.opt(0)
                    if (sub is JSONArray && sub.length() > 0 && sub.opt(0) is JSONArray) {
                        // MultiPolygon: coordinates[0][0]
                        ring = sub
                    } else {
                        // Polygon: coordinates[0]
                        ring = first
                    }
                }
            }

            if (ring != null && ring.length() > 0) {
                var sumLat = 0.0
                var sumLng = 0.0
                var count = 0
                for (i in 0 until ring.length()) {
                    val pt = ring.optJSONArray(i)
                    if (pt != null && pt.length() >= 2) {
                        val lng = pt.optDouble(0)
                        val lat = pt.optDouble(1)
                        if (lat != 0.0 && lng != 0.0) {
                            sumLat += lat
                            sumLng += lng
                            count++
                        }
                    }
                }
                if (count > 0) {
                    return Pair(sumLat / count, sumLng / count)
                }
            }
        } catch (e: Exception) {
            Log.e("EditLahan", "Error calculate centroid: ${e.message}")
        }
        return null
    }

    private fun openGoogleMaps(lat: Double?, lng: Double?, nama: String, lokasi: String) {
        try {
            val uri = if (lat != null && lng != null) {
                "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(nama)})"
            } else if (lokasi.isNotEmpty() && lokasi != "-") {
                "geo:0,0?q=${Uri.encode(lokasi)}"
            } else {
                CustomAlert.showError(this, "Info", "Lokasi atau koordinat lahan belum tersedia")
                return
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            intent.setPackage("com.google.android.apps.maps")
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                val fallbackUrl = if (lat != null && lng != null) {
                    "https://www.google.com/maps/search/?api=1&query=$lat,$lng"
                } else {
                    "https://www.google.com/maps/search/?api=1&query=${Uri.encode(lokasi)}"
                }
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl))
                startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Log.e("EditLahan", "Gagal buka maps", e)
            CustomAlert.showError(this, "Gagal", "Tidak dapat membuka aplikasi peta")
        }
    }

    private fun updateLahan(id: Int, tahun: String, noSurat: String) {
        binding.btnSimpan.isEnabled = false
        binding.btnSimpan.text = "Menyimpan..."

        PetaniApi.updateLahan(id, tahun, noSurat, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.btnSimpan.isEnabled = true
                    binding.btnSimpan.text = "Simpan Perubahan"
                    CustomAlert.showError(this@EditLahanActivity, "Gagal", "Koneksi bermasalah: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    binding.btnSimpan.isEnabled = true
                    binding.btnSimpan.text = "Simpan Perubahan"

                    if (response.isSuccessful) {
                        // Perbarui data di list lokal & tampilan detail
                        val idx = lahanList.indexOfFirst { it.id == id }
                        if (idx != -1) {
                            lahanList[idx].tahunTanam = tahun
                            lahanList[idx].noSurat = noSurat
                            displayLahanDetail(lahanList[idx])
                        }

                        CustomAlert.showSuccess(this@EditLahanActivity, "Berhasil", "Data lahan berhasil diperbarui")
                    } else {
                        val body = response.body?.string()
                        var errorMsg = "Gagal menyimpan data lahan"
                        try {
                            if (body != null) {
                                val json = JSONObject(body)
                                errorMsg = json.optString("message", errorMsg)
                            }
                        } catch (e: Exception) {}

                        CustomAlert.showError(this@EditLahanActivity, "Gagal", errorMsg)
                    }
                }
            }
        })
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun openGoogleMaps(lat: Double, lng: Double, lahanNama: String) {
            runOnUiThread {
                this@EditLahanActivity.openGoogleMaps(lat, lng, lahanNama, "")
            }
        }
    }
}
