package com.example.notasawit.ProfilPetani

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityEditProfilPetaniBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class EditProfilPetaniActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfilPetaniBinding
    private val sharedPref by lazy { getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE) }
    private val petaniId by lazy { sharedPref.getInt("petani_id", 0) }

    private var desaList = mutableListOf<Desa>()
    private var selectedDesaId: Int? = null

    data class Desa(val id: Int, val name: String) {
        override fun toString(): String = name
    }

    private var selectedImageFile: java.io.File? = null

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val tempFile = java.io.File(cacheDir, "temp_profile_image.jpg")
                val outputStream = java.io.FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()

                selectedImageFile = tempFile
                binding.imgFotoProfil.visibility = View.VISIBLE
                Glide.with(this).load(tempFile).into(binding.imgFotoProfil)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfilPetaniBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnSimpan.setOnClickListener {
            val nama = binding.etNama.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val noHp = binding.etNoHp.text.toString().trim()
            val alamat = binding.etAlamat.text.toString().trim()

            if (nama.isEmpty() || username.isEmpty() || email.isEmpty() || noHp.isEmpty() || selectedDesaId == null) {
                Toast.makeText(this, "Harap lengkapi semua data wajib (*)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnSimpan.isEnabled = false
            binding.btnSimpan.text = "Menyimpan..."

            PetaniApi.updatePetani(
                petaniId, nama, username, email, noHp, selectedDesaId!!, alamat, selectedImageFile,
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            binding.btnSimpan.isEnabled = true
                            binding.btnSimpan.text = "Simpan Perubahan"
                            Toast.makeText(this@EditProfilPetaniActivity, "Gagal menyimpan data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        val bodyString = response.body?.string()
                        var message = "Profil berhasil diperbarui"
                        try {
                            if (bodyString != null) {
                                val json = org.json.JSONObject(bodyString)
                                message = json.optString("message", message)
                                
                                val dataObj = json.optJSONObject("data")
                                if (dataObj != null) {
                                    val sharedPrefUpdate = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
                                    with(sharedPrefUpdate.edit()) {
                                        putString("namaPetani", dataObj.optString("petani_nama", ""))
                                        putString("profilPetani", dataObj.optString("petani_profil", ""))
                                        apply()
                                    }
                                }
                            }
                        } catch (e: Exception) {}
                        
                        runOnUiThread {
                            binding.btnSimpan.isEnabled = true
                            binding.btnSimpan.text = "Simpan Perubahan"
                            if (response.isSuccessful) {
                                Toast.makeText(this@EditProfilPetaniActivity, message, Toast.LENGTH_LONG).show()
                                finish()
                            } else {
                                Toast.makeText(this@EditProfilPetaniActivity, "Gagal menyimpan data: $message", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            )
        }

        binding.btnUbahFotoProfilText.setOnClickListener { pickImageLauncher.launch("image/*") }
        binding.btnUbahFotoProfilIkon.setOnClickListener { pickImageLauncher.launch("image/*") }

        if (petaniId != 0) {
            loadDesa()
        } else {
            Toast.makeText(this, "Petani ID tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadDesa() {
        PetaniApi.getDesa(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EditProfilPetaniActivity, "Gagal memuat desa", Toast.LENGTH_SHORT).show()
                    fetchDataProfil(petaniId)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    try {
                        val jsonArray = JSONObject(body!!).getJSONArray("data")
                        for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            desaList.add(Desa(obj.getInt("desa_id"), obj.getString("desa_nama")))
                        }
                        
                        runOnUiThread {
                            val adapter = ArrayAdapter(
                                this@EditProfilPetaniActivity,
                                android.R.layout.simple_dropdown_item_1line,
                                desaList
                            )
                            binding.spinnerDesa.setAdapter(adapter)
                            
                            binding.spinnerDesa.setOnItemClickListener { _, _, position, _ ->
                                selectedDesaId = desaList[position].id
                            }
                            
                            fetchDataProfil(petaniId)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        runOnUiThread { fetchDataProfil(petaniId) }
                    }
                } else {
                    runOnUiThread { fetchDataProfil(petaniId) }
                }
            }
        })
    }

    private fun fetchDataProfil(id: Int) {
        PetaniApi.getDetailPetani(id, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@EditProfilPetaniActivity, "Gagal mengambil data", Toast.LENGTH_SHORT).show()
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
                                val nama = data.optString("petani_nama", "")
                                val username = data.optString("petani_username", "")
                                val email = data.optString("petani_email", "")
                                val noHp = data.optString("petani_no_hp", "")
                                val alamat = data.optString("petani_alamat", "")
                                val desaId = data.optInt("desa_id", 0)
                                val profilUrl = data.optString("petani_profil", "")

                                runOnUiThread {
                                    binding.etNama.setText(nama)
                                    binding.etUsername.setText(username)
                                    binding.etEmail.setText(email)
                                    binding.etNoHp.setText(noHp)
                                    binding.etAlamat.setText(alamat)

                                    if (desaId != 0) {
                                        val selectedDesa = desaList.find { it.id == desaId }
                                        if (selectedDesa != null) {
                                            binding.spinnerDesa.setText(selectedDesa.name, false)
                                            selectedDesaId = selectedDesa.id
                                        }
                                    }
                                    
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
                                        binding.imgFotoProfil.visibility = View.VISIBLE
                                        val fullUrl = if (!profilUrl.startsWith("http")) {
                                            "http://160.187.144.157/storage/$profilUrl"
                                        } else {
                                            profilUrl
                                        }
                                        Glide.with(this@EditProfilPetaniActivity)
                                            .load(fullUrl)
                                            .into(binding.imgFotoProfil)
                                    } else {
                                        binding.imgFotoProfil.visibility = View.GONE
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("EditProfilPetani", "Error parsing JSON: ${e.message}")
                        }
                    }
                }
            }
        })
    }
}
