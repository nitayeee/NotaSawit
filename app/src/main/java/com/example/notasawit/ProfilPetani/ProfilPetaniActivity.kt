package com.example.notasawit.ProfilPetani

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import org.json.JSONObject
import java.io.IOException

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
            com.example.notasawit.utils.CustomAlert.showError(this, "Gagal", "Petani ID tidak ditemukan")
        }
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
                    com.example.notasawit.utils.CustomAlert.showError(this@ProfilPetaniActivity, "Gagal", "Gagal mengambil data: ${e.message}")
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
                                            "http://160.187.144.157/storage/$profilUrl"
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
                                com.example.notasawit.utils.CustomAlert.showError(this@ProfilPetaniActivity, "Gagal", "Format response tidak sesuai")
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

                            if (dataArray != null) {
                                for (i in 0 until dataArray.length()) {
                                    val lahanObj = dataArray.optJSONObject(i)
                                    val luas = lahanObj?.optDouble("lahan_luas", 0.0) ?: 0.0
                                    totalLuas += luas
                                }
                            }

                            runOnUiThread {
                                binding.tvJumlahLahan.text = "$jumlahLahan"
                                binding.tvTotalLuas.text = if (totalLuas > 0) String.format("%.2f", totalLuas) else "-"
                                binding.tvInfoJumlahLahan.text = "$jumlahLahan Lahan"
                                binding.tvInfoTotalLuas.text = if (totalLuas > 0) String.format("%.2f Hektar", totalLuas) else "- Hektar"
                            }
                        } catch (e: Exception) {
                            Log.e("ProfilPetani", "Error parsing Lahan: ${e.message}")
                        }
                    }
                }
            }
        })
    }
}