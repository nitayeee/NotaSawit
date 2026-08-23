package com.example.notasawit.Autentikasi.Masuk

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityLupaPinBinding
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Utils.CustomAlert
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class LupaPinActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLupaPinBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLupaPinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnResetPin.setOnClickListener {
            resetPin()
        }
    }

    private fun resetPin() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val pinBaru = binding.etPinBaru.text.toString().trim()
        val konfirmasiPin = binding.etKonfirmasiPin.text.toString().trim()

        if (username.isEmpty() || email.isEmpty() || pinBaru.isEmpty() || konfirmasiPin.isEmpty()) {
            Toast.makeText(this, "Semua kolom harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        if (pinBaru.length != 6) {
            Toast.makeText(this, "PIN baru harus 6 digit angka", Toast.LENGTH_SHORT).show()
            return
        }

        if (pinBaru != konfirmasiPin) {
            Toast.makeText(this, "PIN baru dan Konfirmasi PIN tidak cocok", Toast.LENGTH_SHORT).show()
            return
        }

        PetaniApi.lupaPin(username, email, pinBaru, object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    CustomAlert.showError(
                        this@LupaPinActivity,
                        "Gagal",
                        "Tidak dapat terhubung ke server: ${e.message}"
                    )
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val json = response.body?.string()
                if (response.isSuccessful && json != null) {
                    try {
                        val jsonObject = org.json.JSONObject(json)
                        val success = jsonObject.getBoolean("success")
                        val message = jsonObject.getString("message")
                        
                        runOnUiThread {
                            if (success) {
                                CustomAlert.showSuccess(
                                    this@LupaPinActivity,
                                    "Berhasil",
                                    message
                                )
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    finish()
                                }, 1500)
                            } else {
                                CustomAlert.showError(
                                    this@LupaPinActivity,
                                    "Gagal",
                                    message
                                )
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            CustomAlert.showError(
                                this@LupaPinActivity,
                                "Error",
                                "Format respons tidak valid"
                            )
                        }
                    }
                } else {
                    runOnUiThread {
                        CustomAlert.showError(
                            this@LupaPinActivity,
                            "Gagal",
                            "Data username dan email tidak cocok atau tidak ditemukan."
                        )
                    }
                }
            }
        })
    }
}
