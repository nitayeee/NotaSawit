package com.example.notasawit.Autentikasi.Masuk

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.Autentikasi.Daftar.DaftarActivity
import com.example.notasawit.Autentikasi.Daftar.OtpActivity
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityMasukBinding
import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class MasukActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMasukBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMasukBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.navToDaftar.setOnClickListener {
            val intent = Intent(this@MasukActivity, DaftarActivity::class.java)
            startActivity(intent)
        }
        // 1. Cek apakah user ini sudah pernah login sebelumnya
        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val sudahPernahLogin = sharedPref.getBoolean("sudah_pernah_login", true)

        binding.btnFingerprint.setOnClickListener {
            if (sudahPernahLogin) {
                // Jika sudah pernah login, cek apakah HP mendukung sidik jari
                checkAndShowFingerprint()
            }
        }


    }

    private fun checkAndShowFingerprint() {
        val biometricManager = BiometricManager.from(this)

        // Cek apakah hardware ada dan sidik jari sudah didaftarkan di pengaturan HP
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt()
            }
            else -> {
                // Jika HP tidak mendukung atau sidik jari belum diatur,
                // biarkan user pakai Email & PIN saja (tidak muncul apa-apa)
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // LOGIN SUKSES! Langsung pindah ke Beranda
                    startActivity(Intent(this@MasukActivity, DaftarActivity::class.java))
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // Jika user menekan tombol "Batal", biarkan mereka pakai PIN
                    Toast.makeText(this@MasukActivity, "Silakan masukkan PIN Anda", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Login Cepat")
            .setSubtitle("Gunakan sidik jari untuk masuk ke NOTASAWIT")
            .setNegativeButtonText("Gunakan PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}