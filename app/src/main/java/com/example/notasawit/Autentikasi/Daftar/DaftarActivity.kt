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
import com.example.notasawit.supabase.SupabaseHelper
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
        val emailPetani = sharedPref.getString("emailPetani", "")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnKirimOtp.setOnClickListener {
            val emailUser = binding.etEmailOtp.text.toString().trim()

            if (emailUser.isNotEmpty()) {
                // Karena ini proses jaringan (network), harus dijalankan di dalam Coroutine
                lifecycleScope.launch {
                    try {
                        // 1. Meminta Supabase mengirimkan OTP ke email tersebut
                        SupabaseHelper.client.auth.signInWith(OTP) {
                            email = emailUser
                        }
                        val inputEmail =binding.etEmailOtp.text.toString().trim()

                        // 2. Kalau sukses, pindah ke halaman untuk memasukkan 6 digit angka
                        val editor = sharedPref.edit()
                        editor.putString("emailPetani",inputEmail)
                        editor.apply()

                        val intent = Intent(this@DaftarActivity, OtpActivity::class.java)
                        intent.putExtra("EMAIL_USER", emailUser) // Bawa emailnya ke halaman sebelah
                        startActivity(intent)

                    } catch (e: Exception) {
                        // Tampilkan pesan error kalau gagal (misal: tidak ada internet)
                        Toast.makeText(this@DaftarActivity, "Gagal kirim OTP: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // 3. TAMBAHAN: Beri tahu user kalau emailnya kosong
                Toast.makeText(this@DaftarActivity, "Email tidak boleh kosong!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}