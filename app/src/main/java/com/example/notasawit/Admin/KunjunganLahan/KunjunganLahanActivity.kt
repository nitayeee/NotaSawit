package com.example.notasawit.Admin.KunjunganLahan

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.ActivityKunjunganLahanBinding

class KunjunganLahanActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKunjunganLahanBinding
    lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityKunjunganLahanBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        database = AppDatabase.getDatabase(this)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DashboardKunjunganFragment())
                .commit()
        }

        binding.progressBarKL.visibility = android.view.View.GONE

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment is DashboardKunjunganFragment || supportFragmentManager.backStackEntryCount == 0) {
                binding.progressBarKL.visibility = android.view.View.GONE
            } else {
                binding.progressBarKL.visibility = android.view.View.VISIBLE
            }
        }

        binding.btnBack.setOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish() 
            }
        }
        
        binding.btnRiwayat.setOnClickListener {
            Toast.makeText(this, "Membuka Riwayat Kunjungan Lahan...", Toast.LENGTH_SHORT).show()
        }
    }
    
    fun updateProgress(currentStep: Int) {
        binding.progressBarKL.progress = currentStep
    }

    fun navigateTo(fragment: Fragment, progress: Int) {
        if (fragment is DashboardKunjunganFragment) {
            binding.progressBarKL.visibility = android.view.View.GONE
        } else {
            binding.progressBarKL.visibility = android.view.View.VISIBLE
        }
        updateProgress(progress)
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    
    fun navigateBack(progress: Int) {
        updateProgress(progress)
        supportFragmentManager.popBackStack()
    }
}
