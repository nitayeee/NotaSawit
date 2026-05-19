package com.example.notasawit.Autentikasi.Daftar.DataDiri

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityDaftarBinding
import com.example.notasawit.databinding.ActivityDataDiriBinding
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator

class DataDiriActivity : AppCompatActivity() {
    private lateinit var viewPager: ViewPager2
    private lateinit var dotsIndicator: DotsIndicator
    private lateinit var binding: ActivityDataDiriBinding
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
        viewPager = binding.tutorialMessageViewPager
        dotsIndicator = binding.dotIndicator

        // Set Adapter
        val adapter = DataDiriAdapter(this)
        viewPager.adapter = adapter

        // Opsional: Matikan swipe manual jika ingin user klik tombol "Next"
        // viewPager.isUserInputEnabled = false


    }
    fun nextStep() {
        viewPager.currentItem = viewPager.currentItem + 1
    }

}