package com.example.notasawit.Admin.ProfilAdmin

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.databinding.ActivityEditProfilAdminBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import android.graphics.BitmapFactory

import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.R
import com.bumptech.glide.Glide

class EditProfilAdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfilAdminBinding

    private val sharedPref by lazy {
        getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
    }

    private var selectedImageFile: java.io.File? = null

    private val pickImageLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                val inputStream = contentResolver.openInputStream(uri)
                val tempFile = java.io.File(cacheDir, "temp_admin_profile_${System.currentTimeMillis()}.jpg")
                val outputStream = java.io.FileOutputStream(tempFile)
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                
                val bitmap = BitmapFactory.decodeFile(tempFile.absolutePath)
                val compressedFile = java.io.File(cacheDir, "compressed_admin_profile_${System.currentTimeMillis()}.jpg")
                val compressStream = java.io.FileOutputStream(compressedFile)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, compressStream)
                compressStream.flush()
                compressStream.close()
                
                tempFile.delete()
                
                val finalFile = compressedFile
                val fileSizeInMB = finalFile.length() / (1024.0 * 1024.0)
                
                if (fileSizeInMB > 1.0) {
                    Toast.makeText(this, "Ukuran gambar maksimal 1MB", Toast.LENGTH_SHORT).show()
                    selectedImageFile = null
                    return@registerForActivityResult
                }
                
                selectedImageFile = finalFile
                binding.tvInitials.visibility = android.view.View.GONE
                Glide.with(this).load(finalFile).into(binding.imgFotoProfil)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditProfilAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnGantiFoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        loadData()

        binding.btnSimpan.setOnClickListener {
            simpanProfil()
        }
    }

    private fun loadData() {
        val nama = sharedPref.getString("admin_nama", "") ?: ""
        val username = sharedPref.getString("admin_username", "") ?: ""
        val email = sharedPref.getString("admin_email", "") ?: ""
        val fotoPath = sharedPref.getString("admin_foto", "") ?: ""

        binding.etNama.setText(nama)
        binding.etUsername.setText(username)
        binding.etEmail.setText(email)

        if (fotoPath.isNotEmpty() && fotoPath != "null") {
            binding.tvInitials.visibility = android.view.View.GONE
            com.bumptech.glide.Glide.with(this)
                .load("http://160.187.144.157/storage/profil/$fotoPath")
                .centerCrop()
                .into(binding.imgFotoProfil)
        } else {
            binding.tvInitials.visibility = android.view.View.VISIBLE
            binding.tvInitials.text = if (nama.isNotEmpty()) nama.substring(0, 1).uppercase() else "A"
            binding.imgFotoProfil.setImageDrawable(null)
            // Need to tint back to default if no image, but we just set drawable to null so it's fine.
        }
    }

    private fun simpanProfil() {
        val adminId = sharedPref.getInt("user_id", -1)
        if (adminId == -1) {
            Toast.makeText(this, "Admin ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val nama = binding.etNama.text.toString().trim()
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        // Validate
        if (nama.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Nama dan Username tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSimpan.isEnabled = false
        binding.btnSimpan.text = "Menyimpan..."

        PetaniApi.updateProfilAdmin(
            adminId = adminId,
            nama = nama,
            username = username,
            email = email,
            noHp = "", // Not used in this basic layout
            jk = "", // Removed
            imageFile = selectedImageFile,
            callback = object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        binding.btnSimpan.isEnabled = true
                        binding.btnSimpan.text = "Simpan Perubahan"
                        Toast.makeText(this@EditProfilAdminActivity, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        binding.btnSimpan.isEnabled = true
                        binding.btnSimpan.text = "Simpan Perubahan"
                    }
                    val responseData = response.body?.string()
                    if (response.isSuccessful && responseData != null) {
                        try {
                            val jsonObject = JSONObject(responseData)
                            if (jsonObject.getBoolean("success")) {
                                val dataObj = jsonObject.optJSONObject("data")
                                if (dataObj != null) {
                                    sharedPref.edit().apply {
                                        putString("admin_nama", dataObj.optString("user_nama", nama))
                                        putString("admin_username", dataObj.optString("user_username", username))
                                        putString("admin_email", dataObj.optString("user_email", email))
                                        putString("admin_foto", dataObj.optString("user_profil", ""))
                                    }.apply()
                                }
                                
                                runOnUiThread {
                                    Toast.makeText(this@EditProfilAdminActivity, "Profil berhasil diupdate", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                            } else {
                                runOnUiThread {
                                    Toast.makeText(this@EditProfilAdminActivity, jsonObject.optString("message", "Gagal menyimpan data"), Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@EditProfilAdminActivity, "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        )
    }
}
