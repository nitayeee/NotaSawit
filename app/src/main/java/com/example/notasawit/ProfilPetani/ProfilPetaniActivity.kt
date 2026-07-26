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

        if (petaniId != 0) {
            fetchDataProfil(petaniId)
            fetchDataLahan(petaniId)
        } else {
            Toast.makeText(this, "Petani ID tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchDataProfil(id: Int) {
        PetaniApi.getDetailPetani(id, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProfilPetaniActivity, "Gagal mengambil data: ${e.message}", Toast.LENGTH_SHORT).show()
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

                                runOnUiThread {
                                    binding.tvNamaProfile.text = nama
                                    binding.tvNama.text = nama
                                    binding.tvUsername.text = username
                                    binding.tvEmail.text = email
                                    binding.tvJenisKelamin.text = jenisKelamin
                                    
                                    if (profilUrl.isNotEmpty() && profilUrl != "null") {
                                        val fullUrl = if (!profilUrl.startsWith("http")) {
                                            "http://160.187.144.157/storage/$profilUrl"
                                        } else {
                                            profilUrl
                                        }
                                        Glide.with(this@ProfilPetaniActivity)
                                            .load(fullUrl)
                                            .placeholder(R.drawable.dummy_profile)
                                            .error(R.drawable.dummy_profile)
                                            .into(binding.imgFotoProfil)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("ProfilPetani", "Error parsing JSON: ${e.message}")
                            runOnUiThread {
                                Toast.makeText(this@ProfilPetaniActivity, "Format response tidak sesuai", Toast.LENGTH_SHORT).show()
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

                            runOnUiThread {
                                binding.tvJumlahLahan.text = "$jumlahLahan"
                                binding.tvTotalLuas.text = "-" // Tetap strip jika API belum menyediakan total luas hektar
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