package com.example.notasawit.Autentikasi.Daftar.DataDiri

import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityDataDiriBinding

class DataDiriActivity : AppCompatActivity() {

    // Jadikan binding public agar bisa diakses oleh Fragment jika diperlukan
    lateinit var binding: ActivityDataDiriBinding
    private lateinit var viewPager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityDataDiriBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Di dalam onCreate() DataDiriActivity
        viewPager = binding.tutorialMessageViewPager

// Pastikan kirim 'this' sebagai context Activity
        val adapter = DataDiriAdapter(this)
        viewPager.adapter = adapter

// Matikan swipe biar terkontrol lewat tombol
        viewPager.isUserInputEnabled = false

        // Logika otomatis mengubah warna indikator (1-2-3) saat ViewPager digeser
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateStepIndicator(position)
            }
        })
    }

    /**
     * Fungsi untuk mengatur warna lingkaran dan garis secara otomatis berdasarkan posisi halaman
     */
    private fun updateStepIndicator(position: Int) {
        // Ambil warna hijau dari resourcemor agri_green_dark Anda
        val colorActiveLine = ContextCompat.getColor(this, R.color.agri_green_dark)
        val colorInactiveLine = Color.parseColor("#D6D6D6")

        when (position) {
            0 -> {
                // Tahap 1 Aktif
                binding.tvStep1.isSelected = true
                binding.tvStep2.isSelected = false
                binding.tvStep3.isSelected = false

                binding.lineStep1.setBackgroundColor(colorInactiveLine)
                binding.lineStep2.setBackgroundColor(colorInactiveLine)
            }
            1 -> {
                // Tahap 2 Aktif (Tahap 1 juga tetap menyala hijau)
                binding.tvStep1.isSelected = true
                binding.tvStep2.isSelected = true
                binding.tvStep3.isSelected = false

                binding.lineStep1.setBackgroundColor(colorActiveLine)
                binding.lineStep2.setBackgroundColor(colorInactiveLine)
            }
            2 -> {
                // Tahap 3 Aktif (Semua menyala hijau)
                binding.tvStep1.isSelected = true
                binding.tvStep2.isSelected = true
                binding.tvStep3.isSelected = true

                binding.lineStep1.setBackgroundColor(colorActiveLine)
                binding.lineStep2.setBackgroundColor(colorActiveLine)
            }
        }
    }

    // Fungsi yang dipanggil dari Fragment saat tombol "Lanjut" ditekan
    // Fungsi nextStep yang dipanggil Fragment
    fun nextStep() {
        val current = viewPager.currentItem
        // Paksa ViewPager pindah ke halaman berikutnya dengan animasi smooth
        viewPager.setCurrentItem(current + 1, true)
    }
}