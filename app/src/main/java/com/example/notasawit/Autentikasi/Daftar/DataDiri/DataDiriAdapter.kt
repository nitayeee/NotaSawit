package com.example.notasawit.Autentikasi.Daftar.DataDiri

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class DataDiriAdapter (activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3 // Ada 3 tahap

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> DataDiri1Fragment()
            1 -> DataDiri2Fragment()
            2 -> DataDiri3Fragment()
            else -> DataDiri1Fragment()
        }
    }
}