package com.example.notasawit.Autentikasi.Daftar

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityDaftarBinding
import com.example.notasawit.Network.PetaniApi
import kotlinx.coroutines.launch

// 1. TAMBAHKAN IMPORT SUPABASE INI
// GANTI MENJADI INI:
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.OTP

// Catatan: Pastikan kamu sudah membuat instance/object 'supabase' di project kamu.
// Jika kamu menaruhnya di file lain (misal: SupabaseClient.kt), import ke sini:
// import com.example.notasawit.SupabaseClient.supabase

class DaftarActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDaftarBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDaftarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val sp_emailPetani = sharedPref.getString("emailPetani", "")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnKirimOtp.setOnClickListener {
            val emailUser = binding.etEmailOtp.text.toString().trim()

            if (emailUser.isNotEmpty()) {
                binding.btnKirimOtp.isEnabled = false
                
                // Cek email ke server (sementara dengan getAllPetani)
                PetaniApi.getAllPetani(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        runOnUiThread {
                            binding.btnKirimOtp.isEnabled = true
                            Toast.makeText(this@DaftarActivity, "Gagal mengecek email: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        var emailExists = false
                        if (response.isSuccessful) {
                            val body = response.body?.string()
                            if (body != null) {
                                try {
                                    val jsonObject = org.json.JSONObject(body)
                                    val dataArray = jsonObject.getJSONArray("data")
                                    for (i in 0 until dataArray.length()) {
                                        val item = dataArray.getJSONObject(i)
                                        if (item.has("petani_email") && item.getString("petani_email") == emailUser) {
                                            emailExists = true
                                            break
                                        }
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }

                        runOnUiThread {
                            binding.btnKirimOtp.isEnabled = true
                            if (emailExists) {
                                android.app.AlertDialog.Builder(this@DaftarActivity)
                                    .setTitle("Email Sudah Terdaftar")
                                    .setMessage("Email yang Anda masukkan sudah terdaftar. Silakan login menggunakan akun Anda.")
                                    .setPositiveButton("Login") { _, _ ->
                                        startActivity(Intent(this@DaftarActivity, com.example.notasawit.Autentikasi.Masuk.MasukActivity::class.java))
                                        finish()
                                    }
                                    .setNegativeButton("Batal", null)
                                    .show()
                            } else {
                                lifecycleScope.launch {
                                    try {
                                        val inputEmail = binding.etEmailOtp.text.toString().trim()
                                        val editor = sharedPref.edit()
                                        editor.putString("emailPetani", inputEmail)
                                        editor.apply()

                                        val intent = Intent(this@DaftarActivity, OtpActivity::class.java)
                                        intent.putExtra("EMAIL_USER", emailUser)
                                        startActivity(intent)

                                    } catch (e: Exception) {
                                        Toast.makeText(this@DaftarActivity, "Gagal kirim OTP: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                })
            } else {
                Toast.makeText(this@DaftarActivity, "Email tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}