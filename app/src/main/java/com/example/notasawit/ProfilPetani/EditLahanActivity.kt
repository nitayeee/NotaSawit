package com.example.notasawit.ProfilPetani

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityEditLahanBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import com.example.notasawit.Utils.CustomAlert

class EditLahanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditLahanBinding
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE) }
    private val petaniId by lazy { sharedPref.getInt("petani_id", 0) }

    // List untuk menyimpan data lahan
    private val lahanList = mutableListOf<LahanData>()
    private var selectedLahanId: Int = -1

    data class LahanData(
        val id: Int,
        val nama: String,
        val lokasi: String,
        val luas: Double,
        val tahunTanam: String,
        val noSurat: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditLahanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        // Fetch data lahan dari server
        fetchLahan()

        // Handle spinner selection
        binding.spinnerLahan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position >= 0 && position < lahanList.size) {
                    val selectedLahan = lahanList[position]
                    selectedLahanId = selectedLahan.id
                    
                    // Isi form dengan data yang ada
                    binding.etTahunTanam.setText(if (selectedLahan.tahunTanam != "null" && selectedLahan.tahunTanam.isNotEmpty()) selectedLahan.tahunTanam else "")
                    binding.etNoSurat.setText(if (selectedLahan.noSurat != "null" && selectedLahan.noSurat.isNotEmpty()) selectedLahan.noSurat else "")
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedLahanId = -1
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
                                    val obj = dataArray.optJSONObject(i)
                                    if (obj != null) {
                                        lahanList.add(
                                            LahanData(
                                                id = obj.optInt("lahan_id", -1),
                                                nama = obj.optString("lahan_nama", "-"),
                                                lokasi = obj.optString("lahan_lokasi", "-"),
                                                luas = if (obj.has("lahan_luas") && !obj.isNull("lahan_luas")) obj.optDouble("lahan_luas", 0.0) else obj.optDouble("luas_lahan", 0.0),
                                                tahunTanam = obj.optString("tahun_tanam", ""),
                                                noSurat = obj.optString("lahan_no_surat", "")
                                            )
                                        )
                                    }
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
        // Buat list nama lahan untuk ditampilkan di spinner
        val namaLahanList = lahanList.map { "${it.nama} (${it.lokasi})" }
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, namaLahanList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        
        binding.spinnerLahan.adapter = adapter
    }

    private fun updateLahan(id: Int, tahun: String, noSurat: String) {
        // Disable button while loading
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
                        CustomAlert.showSuccess(this@EditLahanActivity, "Berhasil", "Data lahan berhasil diperbarui")
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            finish()
                        }, 1500)
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
}
