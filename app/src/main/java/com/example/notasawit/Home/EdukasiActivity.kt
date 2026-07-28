package com.example.notasawit.Home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityEdukasiBinding

class EdukasiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEdukasiBinding
    private lateinit var adapter: EdukasiAdapter
    private val modulList = mutableListOf<EdukasiModul>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEdukasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        setupDummyData()
    }

    override fun onResume() {
        super.onResume()
        // Update progress saat kembali dari halaman detail
        updateProgressData()
    }

    private fun setupDummyData() {
        // Modul 1
        modulList.add(EdukasiModul(1, "Modul 1: Asosiasi", "Profil Asosiasi Pekebun Swadaya"))
        
        // Modul 2 - 12 (Coming soon)
        for (i in 2..12) {
            modulList.add(EdukasiModul(i, "Modul $i", "Materi Segera Hadir"))
        }

        adapter = EdukasiAdapter(modulList) { modul ->
            val intent = Intent(this, DetailEdukasiActivity::class.java)
            intent.putExtra("MODUL_ID", modul.id)
            intent.putExtra("MODUL_TITLE", modul.title)
            startActivity(intent)
        }
        
        binding.rvEdukasi.adapter = adapter
    }

    private fun updateProgressData() {
        val userPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val petaniId = userPref.getInt("petani_id", -1)
        
        val sharedPref = getSharedPreferences("EDUKASI_PROGRESS", Context.MODE_PRIVATE)
        for (i in modulList.indices) {
            val prefKey = "progress_modul_${modulList[i].id}_user_$petaniId"
            val progress = sharedPref.getInt(prefKey, 0)
            modulList[i].progress = progress
        }
        if (::adapter.isInitialized) {
            adapter.notifyDataSetChanged()
        }
    }
}
