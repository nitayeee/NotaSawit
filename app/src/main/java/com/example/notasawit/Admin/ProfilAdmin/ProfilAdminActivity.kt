package com.example.notasawit.Admin.ProfilAdmin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.databinding.ActivityProfilAdminBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.R

class ProfilAdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfilAdminBinding

    private val sharedPref by lazy {
        getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityProfilAdminBinding.inflate(layoutInflater)
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
            val intent = Intent(this, EditProfilAdminActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        fetchAdminProfile()
    }

    private fun fetchAdminProfile() {
        val adminId = sharedPref.getInt("user_id", -1)
        if (adminId == -1) return

        PetaniApi.getDetailAdmin(adminId, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ProfilAdminActivity, "Gagal mengambil data profil", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                if (response.isSuccessful && responseData != null) {
                    try {
                        val jsonObject = JSONObject(responseData)
                        if (jsonObject.getBoolean("success")) {
                            val data = jsonObject.getJSONObject("data")
                            
                            val nama = data.optString("user_nama", "Admin")
                            val username = data.optString("user_username", "-")
                            val email = data.optString("user_email", "-")
                            val jk = data.optString("user_jenis_kelamin", "-")
                            val fotoPath = data.optString("user_profil", "")

                            runOnUiThread {
                                binding.tvNamaProfile.text = nama
                                binding.tvNama.text = nama
                                binding.tvUsername.text = username
                                binding.tvEmail.text = email
                                
                                if (fotoPath.isNotEmpty() && fotoPath != "null") {
                                    binding.tvInitials.visibility = View.GONE
                                    Glide.with(this@ProfilAdminActivity)
                                        .load("http://160.187.144.157/storage/profil/$fotoPath")
                                        .centerCrop()
                                        .into(binding.imgFotoProfil)
                                } else {
                                    binding.tvInitials.visibility = View.VISIBLE
                                    binding.tvInitials.text = if (nama.isNotEmpty()) nama.substring(0, 1).uppercase() else "A"
                                    binding.imgFotoProfil.setImageDrawable(null)
                                }
                                
                                // Simpan ke shared pref agar Edit Profile bisa pakai
                                sharedPref.edit().apply {
                                    putString("admin_nama", nama)
                                    putString("admin_username", username)
                                    putString("admin_email", email)
                                    putString("admin_foto", fotoPath)
                                }.apply()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        })
    }
}
