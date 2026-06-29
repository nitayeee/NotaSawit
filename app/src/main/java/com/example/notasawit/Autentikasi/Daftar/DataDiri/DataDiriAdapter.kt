package com.example.notasawit.Autentikasi.Daftar.DataDiri

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class DataDiriAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    // 1. Tentukan total jumlah fragment form data diri Anda (misal ada 3 tahap)
    override fun getItemCount(): Int {
        return 3
    }

    // 2. Tentukan fragment mana yang muncul berdasarkan urutan posisi (index dimulai dari 0)
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DataDiri1Fragment() // Halaman pertama (Nama, Usia, dll)
            1 -> DataDiri2Fragment() // Halaman kedua (Wajib Anda buat filenya)
            2 -> DataDiri3Fragment() // Halaman ketiga (Jika ada tahap 3)
            else -> DataDiri1Fragment()
        }
    }
}