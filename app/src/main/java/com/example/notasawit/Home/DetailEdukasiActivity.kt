package com.example.notasawit.Home

import android.content.Context
import android.os.Bundle
import android.view.ViewTreeObserver
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityDetailEdukasiBinding
import android.view.View

class DetailEdukasiActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailEdukasiBinding
    private var modulId: Int = 1
    private var currentProgress: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityDetailEdukasiBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        modulId = intent.getIntExtra("MODUL_ID", 1)
        val modulTitle = intent.getStringExtra("MODUL_TITLE") ?: "Modul Edukasi"

        binding.tvToolbarTitle.text = modulTitle
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Load saved progress
        val userPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val petaniId = userPref.getInt("petani_id", -1)
        val prefKey = "progress_modul_${modulId}_user_$petaniId"
        
        val sharedPref = getSharedPreferences("EDUKASI_PROGRESS", Context.MODE_PRIVATE)
        currentProgress = sharedPref.getInt(prefKey, 0)
        binding.pbReadingProgress.progress = currentProgress
        
        if (currentProgress == 100) {
            binding.layoutSelesai.visibility = View.VISIBLE
        }

        loadContent()
        setupScrollTracker()
    }

    private fun loadContent() {
        if (modulId == 1) {
            binding.tvModulTitle.text = "PROFIL ASOSIASI PEKEBUN SWADAYA KELAPA SAWIT PELALAWAN SIAK"
            val textContent = """
1. Latar Belakang
Asosiasi Pekebun Swadaya Kelapa Sawit Pelalawan Siak merupakan organisasi bagi petani-petani kelapa sawit yang dibina oleh International Finance Coorporation (IFC) dan Musim Mas Group melalui program Indonesia Palm Oil Development for Smallholders (IPODS). Program IPODS telah berjalan sejak tahun 2016 hingga 2020. Tujuan dari program tersebut adalah untuk membantu petani-petani swadaya tanaman kelapa sawit dalam mengoptimalkan produktifitas lahannya melalui 3 faktor, yaitu; akses kepada pengetahuan dan pembinaan, akses kepada pasar internasional (Sertifikasi RSPO) dan akses kepada perbankan. Untuk dapat meningkatkan efektifitas dan menjaga keberlanjutan dari tujuan program IPODS, maka diperlulah dibentuk Asosiasi PSKS Pelalawan Siak.

2. Nama Organisasi
Organisasi ini bernama “Pekebun Swadaya Kelapa Sawit Pelalawan Siak atau disebut juga Asosiasi Pekebun Swadaya Kelapa Sawit Pelalawan Siak”. Asosiasi Pekebun Swadaya Kelapa Sawit Pelalawan Siak dibentuk pada hari Rabu tanggal 25 September 2019.

3. Alamat Kantor
Asosiasi Pekebun Swadaya Kelapa Sawit Pelalawan Siak berkedudukan di Kelurahan Sekijang, Kecamatan Bandar Seikijang, Kabupaten Pelalawan, Provinsi Riau. Asosiasi ini mendapat badan hukum tanggal 20 Januari 2020 dengan NOMOR AHU-0000375.AH.01.07.TAHUN 2020 dan pada tanggal maret 2020 sudah resmi menjadi anggota RSPO dengan nomor keanggotaan: 1-0301-20-000-00.

4. Visi dan Misi Asosiasi PSKS Pelalawan Siak
Visi
• Membentuk petani yang berilmu pengetahuan dan berwawasan lingkungan serta mampu meraih prestasi nasional dan internasional.

Misi
• Melakukan pelatihan-pelatihan tentang budidaya kelapa sawit yang berkelanjutan kepada petani.
• Melakukan pelatihan-pelatihan tentang lingkungan, kesehatan dan keselamatan kerja.
• Membangun jaringan dengan para pihak mulai dari lokal, nasional serta internasional.

5. Contact Person
Ketua Asosiasi : 0813-7813-1447 (Joko Prasetyo)
Wakil Ketua : 0812-7584-671 (Lukman)
Sekretaris : 0812-7587-8847 (Rita Sri Ningsih)
Bendahara : 0813-7396-1477 (Eko Budiono)
Kantor Asosiasi : 0822-8533-5295

6. Struktur Pengurus Asosiasi PSKS Pelalawan Siak Tahun 2025
(Struktur kepengurusan terdiri dari Ketua, Wakil Ketua, Sekretaris, Bendahara, Badan Pengawas, serta pengurus dari beberapa desa)

7. Perjalanan Sertifikasi Asosiasi PSKS Pelalawan Siak
A. Sertifikasi RSPO Tahun 2020
Asosiasi PSKS Pelalawan Siak mengikuti sertifikasi RSPO untuk pertama kali (Initial Audit) pada tahun 2020. berhasil mendapatkan sertifikasi RSPO pertama dengan jumlah anggota sebanyak 367 petani, luas lahan 1424,26 Ha, dan 551 blok. Adapun hasil initial audit yaitu tidak ada temuan namun terdapat 11 Opportunity for Improvement (OFI) yang harus diperbaiki.

B. Sertifikasi ISPO & Annual Survaillance Audit (ASA) I RSPO Tahun 2021
Pada tahun 2021 selain mengikuti Annual Survaillance Audit (ASA) I RSPO, Asosiasi PSKS Pelalawan juga mengikuti sertifikasi ISPO untuk pertama kalinya. Hal ini dilakukan sebagai salah satu bentuk dukungan dan persiapan dalam mengimplementasi Permentan No. 38 Tahun 2020 yang bersifat mandatory pada tahun 2025. Asosiasi PSKS Pelalawan Siak berhasil mendapatkan sertifikasi ISPO pertama dan sertifikasi ASA I RSPO pada tahun 2021 dengan jumlah anggota sebanyak 318 petani, luas lahan 1171,82 Ha, dan 468 blok.
Adapun hasil audit Sertifikasi ISPO tahun 2021 yakni tidak terdapat ketidaksesuaian dan Aosiasi PSKS Pelalawan Siak mendapatkan rekomendasi untuk penerbitan Sertifikat ISPO. Sedangkan pada ASA I RSPO, Asosiasi PSKS Pelalawan Siak mendapatkan 1 peluang perbaikan tentang penjualan kredit CPO.

C. Annual Survaillance Audit (ASA) I ISPO & Annual Survaillance Audit (ASA) II RSPO Tahun 2022
Pada tahun 2022 Asosiasi PSKS Pelalawan mengikuti Annual Survaillance Audit (ASA) II RSPO dan Annual Survaillance Audit (ASA) I ISPO. Hal ini dilakukan untuk mempertahankan sertifikat ISPO dan RSPO yang telah didapat. Untuk tahun 2022, jumlah anggota yang mengikuti ASA I ISPO berbeda dengan jumlah anggota yang mengikuti ASA II RSPO. Adapun jumlah anggota yang mengikuti ASA I ISPO adalah 311 petani dengan luasan 1.145,79 Ha dan jumlah blok 460 blok. Sedangkan jumlah anggota yang mengikuti ASA II RSPO adalah 496 petani dengan luasan 1828,92 Ha dan jumlah blok 724 blok. Adapun hasil audit ASA I ISPO adalah tidak terdapat ketidaksesuaian. Sedangkan ASA II RSPO terdapat 1 OFI (peluang perbaikan).

D. Annual Survaillance Audit (ASA) II ISPO & Annual Survaillance Audit (ASA) III RSPO Tahun 2023
Pada Tahun 2023, Asosiasi PSKS Pelalawan Siak melakukan penambahan anggota sebanyak 257 orang dengan luas lahan 488,31 Ha dan jumlah blok 278 blok dalam proses audit tahun 2023. Hal ini dilakukan agar asosiasi mendapatkan bantuan biaya proses audit tahun 2023. Adapun jumlah petani yang telah mengikuti proses audit tahun 2023 adalah jumlah petani 745 petani dengan luas lahan 2.290.70 Ha, dan jumlah blok 993 blok.

E. Sertifikasi RSPO dan ISPO Tahun 2024
Pada tahun 2024 Asosiasi PSKS Pelalawan Siak melakukan sertifikasi ISPO jumlah anggota sebanyak 433 petani dengan luas lahan 1.514,68 Ha dan 610 blok lahan. Dan sertifikasi RSPO dengan jumlah anggota sebanyak 775 petani dengan luas lahan 2.351,15 Ha dan 1.003 blok lahan.

F. Sertifikasi RSPO dan ISPO Tahun 2025
Pada tahun 2025 Asosiasi PSKS Pelalawan Siak melakukan sertifikasi ISPO jumlah anggota sebanyak 681 petani dengan luas lahan 1.954, 96 Ha dan 885 blok lahan. Dan sertifikasi RSPO dengan jumlah anggota sebanyak 859 petani dengan luas lahan 2.447,40 Ha dan 1.111 blok lahan.

8. Keanggotaan Asosiasi PSKS Pelalawan Siak Tahun 2026
a. Total Anggota 2026
Total anggota Asosiasi PSKS Pelalawan Siak tahun 2026 yakni:
• Jumlah Anggota : 883 Petani
• Luas Lahan : 2475, 27 Ha
• Jumlah Blok : 1128 Blok
            """.trimIndent()
            
            binding.tvModulContent.text = textContent
        } else {
            binding.tvModulTitle.text = "Modul Segera Hadir"
            binding.tvModulContent.text = "Materi untuk modul ini sedang dalam persiapan. Silakan kembali lagi nanti."
            
            // Do not save progress for empty modules
        }
    }

    private fun setupScrollTracker() {
        // Only track progress for actual implemented modules (e.g. Modul 1)
        if (modulId != 1) return

        binding.scrollView.viewTreeObserver.addOnScrollChangedListener {
            val scrollView = binding.scrollView
            val child = scrollView.getChildAt(0)
            
            if (child != null) {
                val scrollY = scrollView.scrollY
                val childHeight = child.height
                val isScrollable = scrollView.height < childHeight

                if (isScrollable) {
                    val maxScroll = childHeight - scrollView.height
                    var percentage = ((scrollY.toFloat() / maxScroll) * 100).toInt()
                    
                    // Allow some buffer to reach 100% easier
                    if (percentage >= 95) {
                        percentage = 100
                    }
                    
                    // Only update if going forward
                    if (percentage > currentProgress) {
                        saveProgress(percentage)
                    }
                } else {
                    // Content is small, instantly 100%
                    if (currentProgress < 100) {
                        saveProgress(100)
                    }
                }
            }
        }
        
        // Check once if it's already completely visible without scrolling
        binding.scrollView.post {
            val child = binding.scrollView.getChildAt(0)
            if (child != null && binding.scrollView.height >= child.height) {
                if (currentProgress < 100) {
                    saveProgress(100)
                }
            }
        }
    }

    private fun saveProgress(percentage: Int) {
        currentProgress = percentage
        binding.pbReadingProgress.progress = currentProgress
        
        val userPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val petaniId = userPref.getInt("petani_id", -1)
        val prefKey = "progress_modul_${modulId}_user_$petaniId"
        
        val sharedPref = getSharedPreferences("EDUKASI_PROGRESS", Context.MODE_PRIVATE)
        sharedPref.edit().putInt(prefKey, percentage).apply()

        if (percentage == 100) {
            binding.layoutSelesai.visibility = View.VISIBLE
        }
    }
}
