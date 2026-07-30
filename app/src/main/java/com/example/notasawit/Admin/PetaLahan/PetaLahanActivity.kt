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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.databinding.ActivityPetaLahanBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class PetaLahanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetaLahanBinding

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
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    if (response.isSuccessful && body != null) {
                        try {
                            val jsonObject = JSONObject(body)
                            val dataArray = jsonObject.getJSONArray("data")
                            val geoJsonArray = JSONArray()

                            if (dataArray.length() > 0) {
                                Log.d("PETA", "Contoh data JSON item pertama: " + dataArray.getJSONObject(0).toString())
                            }

                            for (i in 0 until dataArray.length()) {
                                val item = dataArray.getJSONObject(i)
                                
                                // Cek berbagai kemungkinan nama field
                                var polyStr: String? = null
                                if (item.has("polygon") && !item.isNull("polygon")) {
                                    polyStr = item.getString("polygon")
                                } else if (item.has("geo_json") && !item.isNull("geo_json")) {
                                    polyStr = item.getString("geo_json")
                                } else if (item.has("lahan_polygon") && !item.isNull("lahan_polygon")) {
                                    polyStr = item.getString("lahan_polygon")
                                } else if (item.has("koordinat") && !item.isNull("koordinat")) {
                                    polyStr = item.getString("koordinat")
                                }

                                if (polyStr != null) {
                                    try {
                                        val polyJson = JSONObject(polyStr)
                                        // Sisipkan properti tambahan agar bisa diklik
                                        val properties = JSONObject()
                                        properties.put("lahan_nama", item.optString("lahan_nama", "Lahan"))
                                        polyJson.put("properties", properties)
                                        geoJsonArray.put(polyJson)
                                    } catch (e: Exception) {
                                        Log.e("PETA", "Gagal parse polygon string: " + e.message)
                                    }
                                } else if (item.has("geo_json") && !item.isNull("geo_json")) {
                                    val polyStr = item.getString("geo_json")
                                    try {
                                        val polyJson = JSONObject(polyStr)
                                        val properties = JSONObject()
                                        properties.put("lahan_nama", item.optString("lahan_nama", "Lahan"))
                                        polyJson.put("properties", properties)
                                        geoJsonArray.put(polyJson)
                                    } catch (e: Exception) {
                                        Log.e("PETA", "Gagal parse geo_json string: " + e.message)
                                    }
                                }
                            }

                            // Kirim GeoJSON ke WebView
                            val script = "javascript:renderPolygons(${geoJsonArray.toString()})"
                            binding.webViewPeta.evaluateJavascript(script, null)

                        } catch (e: Exception) {
                            Log.e("PETA", "Error parsing response: " + e.message)
                        }
                    }
                }
            }
        })
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
