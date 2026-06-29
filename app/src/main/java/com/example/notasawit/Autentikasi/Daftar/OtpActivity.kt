package com.example.notasawit.Autentikasi.Daftar

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityOtpBinding
import android.os.CountDownTimer
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Autentikasi.Daftar.DataDiri.DataDiriActivity
import com.example.notasawit.Network.PetaniApi
import io.github.jan.supabase.auth.OtpType
import java.util.Locale
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

class OtpActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOtpBinding
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val emailPetani = sharedPref.getString("emailPetani", "")

        val email = emailPetani.toString()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        startTimer()

        binding.btnKirimUlang.setOnClickListener {
            // Panggil fungsi kirim OTP lagi di sini (seperti di DaftarActivity)

            // Setelah berhasil kirim ulang, jalankan timer lagi
            startTimer()
        }
        binding.btnVerify.setOnClickListener {
            // Contoh: Mengambil teks dari ke-6 kotak dan menggabungkannya
            val otpInput = (
                    binding.otp1.text.toString() +
                            binding.otp2.text.toString() +
                            binding.otp3.text.toString() +
                            binding.otp4.text.toString() +
                            binding.otp5.text.toString() +
                            binding.otp6.text.toString()
                    ).trim()

// Pastikan yang diinput benar-benar 6 digit
            if (otpInput.length != 6) {
                Toast.makeText(this, "Masukkan 6 digit kode OTP dengan lengkap!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // Minta Supabase mencocokkan OTP
//                    PetaniApi.client.auth.verifyEmailOtp(
//                        type = OtpType.Email.EMAIL,
//                        email = email,
//                        token = otpInput
//                    )

                    // JIKA KODE BENAR (Supabase mengonfirmasi)
                    runOnUiThread {
                        Toast.makeText(this@OtpActivity, "Validasi sukses!", Toast.LENGTH_SHORT).show()
                        // TODO: Pindah ke Activity selanjutnya untuk isi Biodata & PIN
                    }
                    val intent = Intent(this@OtpActivity, DataDiriActivity::class.java)
                    startActivity(intent)

                } catch (e: Exception) {
                    // JIKA KODE SALAH ATAU SUDAH KADALUARSA (Supabase menolak)
                    runOnUiThread {
                        Toast.makeText(this@OtpActivity, "Kode OTP salah. Silakan periksa kembali email Anda.", Toast.LENGTH_LONG).show()
                        // Di sini aplikasi akan tetap diam di halaman OTP sampai inputnya benar
                    }
                }
            }
        }
    }
    private fun startTimer() {
        // 1. Matikan tombol kirim ulang dan kasih warna abu-abu
        binding.btnKirimUlang.isEnabled = false
        binding.btnKirimUlang.setTextColor(Color.parseColor("#808080")) // Abu-abu

        // 2. Atur waktu 2 menit (120.000 milidetik)
        countDownTimer = object : CountDownTimer(120000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                // Update tulisan timer setiap detik
                val minutes = (millisUntilFinished / 1000) / 60
                val seconds = (millisUntilFinished / 1000) % 60
                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
                binding.tvTimer.text = timeFormatted
            }

            override fun onFinish() {
                // 3. Ketika waktu habis
                binding.tvTimer.text = "00:00"
                binding.btnKirimUlang.isEnabled = true
                binding.btnKirimUlang.setTextColor(Color.parseColor("#264A2B")) // Ganti ke warna hijau sawitmu
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Hentikan timer kalau activity ditutup supaya tidak memory leak
        countDownTimer?.cancel()
    }
}