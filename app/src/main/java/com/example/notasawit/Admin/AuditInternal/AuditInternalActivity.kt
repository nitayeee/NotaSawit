package com.example.notasawit.Admin.AuditInternal

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.notasawit.R
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.databinding.ActivityAuditInternalBinding

class AuditInternalActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAuditInternalBinding
    lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAuditInternalBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 1. Inisialisasi Room Database
        database = AppDatabase.getDatabase(this)

        // 2. Pasang DashboardAuditFragment sebagai halaman pertama
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, DashboardAuditFragment())
                .commit()
        }

        binding.progressBarAudit.visibility = android.view.View.GONE

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
            if (currentFragment is DashboardAuditFragment || supportFragmentManager.backStackEntryCount == 0) {
                binding.progressBarAudit.visibility = android.view.View.GONE
            } else {
                binding.progressBarAudit.visibility = android.view.View.VISIBLE
            }
        }

        // 3. Atur tombol Back di Toolbar
        binding.btnBack.setOnClickListener {
            // Jika ada fragment di dalam tumpukan (backstack), kembali ke fragment sebelumnya
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                finish() // Jika di halaman pertama, tutup Activity (kembali ke Dashboard)
            }
        }
        // 4. Atur tombol Riwayat di Toolbar
        binding.btnRiwayat.setOnClickListener {
            Toast.makeText(this, "Membuka Riwayat Audit...", Toast.LENGTH_SHORT).show()
        }
    }
    // Fungsi bantuan untuk mengubah progress bar dari Fragment
    fun updateProgress(currentStep: Int) {
        binding.progressBarAudit.progress = currentStep
    }

    fun navigateTo(fragment: Fragment, progress: Int) {
        if (fragment is DashboardAuditFragment) {
            binding.progressBarAudit.visibility = android.view.View.GONE
        } else {
            binding.progressBarAudit.visibility = android.view.View.VISIBLE
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