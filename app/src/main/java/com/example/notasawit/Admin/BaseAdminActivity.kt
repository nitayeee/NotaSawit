package com.example.notasawit.Admin

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.notasawit.Admin.AuditInternal.AuditInternalActivity
import com.example.notasawit.Admin.Beranda.BerandaAdminFragment
import com.example.notasawit.Admin.KunjunganLahan.KunjunganLahanActivity
import com.example.notasawit.Home.BerandaFragment
import com.example.notasawit.ProfilPetani.ProfilPetaniActivity
import com.example.notasawit.ProfilPetani.SetelanFragment
import com.example.notasawit.R
import com.example.notasawit.RiwayatKeuangan.RiwayatActivity
import com.example.notasawit.databinding.ActivityBaseAdminBinding
import com.example.notasawit.databinding.ActivityBaseBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class BaseAdminActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBaseAdminBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBaseAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnApplyWindowInsetsListener(null)
        bottomNav.setPadding(0, 0, 0, 0)
        replaceFragment(BerandaAdminFragment())
        setupBottomNavListener()
    }

    private var isSelectingProgrammatically = false

    private fun setupBottomNavListener() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            if (isSelectingProgrammatically) return@setOnItemSelectedListener true

            when (item.itemId) {
                R.id.home -> {
                    replaceFragment(BerandaAdminFragment())
                    true
                }

                R.id.auditInternal -> {
                    val intent = Intent(this@BaseAdminActivity, AuditInternalActivity::class.java)
                    startActivity(intent)
                    false
                }

                R.id.auditLahan -> {
                    val intent = Intent(this@BaseAdminActivity, KunjunganLahanActivity::class.java)
                    startActivity(intent)
                    false
                }

                R.id.petaLahan -> {
                    val intent = Intent(this@BaseAdminActivity, com.example.notasawit.Admin.PetaLahan.PetaLahanActivity::class.java)
                    startActivity(intent)
                    false
                }

                R.id.lainnya -> {
                    replaceFragment(SetelanFragment())
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

    override fun onResume() {
        super.onResume()
        syncBottomNavSelection()
    }

    private fun syncBottomNavSelection() {
        val currentFragment = supportFragmentManager.findFragmentById(binding.fragmentContainer.id)
        val expectedItemId = when (currentFragment) {
            is BerandaAdminFragment -> R.id.home
            is SetelanFragment -> R.id.lainnya
            else -> R.id.home
        }
        if (binding.bottomNavigation.selectedItemId != expectedItemId) {
            isSelectingProgrammatically = true
            binding.bottomNavigation.selectedItemId = expectedItemId
            isSelectingProgrammatically = false
        }
    }
}