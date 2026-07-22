package com.example.notasawit

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.notasawit.Autentikasi.Masuk.MasukActivity
import com.example.notasawit.Home.BerandaFragment
import com.example.notasawit.ProfilPetani.ProfilPetaniActivity
import com.example.notasawit.RiwayatKeuangan.RiwayatActivity
import com.example.notasawit.databinding.ActivityBaseBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class BaseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBaseBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityBaseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnApplyWindowInsetsListener(null)
        bottomNav.setPadding(0, 0, 0, 0)

        replaceFragment(BerandaFragment())
        binding.bottomNavigation.setOnItemSelectedListener {

            when (it.itemId) {

                R.id.home -> {
                    replaceFragment(BerandaFragment())
                    true
                }

                R.id.notification -> {
                    replaceFragment(com.example.notasawit.Notifikasi.NotificationFragment())
                    true
                }

                R.id.riwayat -> {
                    val intent = Intent(this@BaseActivity, RiwayatActivity::class.java)
                    startActivity(intent)
                    true
                }

                R.id.pengaturan -> {
                    val intent = Intent(this@BaseActivity, ProfilPetaniActivity::class.java)
                    startActivity(intent)
                    true
                }

                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(binding.fragmentContainer.id, fragment)
            //.addToBackStack(null) -> ini kita nonaktifkan agar saat back langsung keluar aplikasi
            .commit()
    }
}