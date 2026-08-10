package com.example.notasawit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.notasawit.Autentikasi.Masuk.MasukActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TutorialActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var btnNext: Button
    private lateinit var tutorialAdapter: TutorialAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_tutorial)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        btnNext = findViewById(R.id.btnNext)

        val tutorialList = listOf(
            TutorialItem(
                R.drawable.ic_note,
                "Selamat Datang di Notasawit",
                "Pencatatan dan pengelolaan data kebun sawit menjadi lebih mudah dan terpusat dalam satu aplikasi."
            ),
            TutorialItem(
                R.drawable.ic_audit,
                "Audit Internal Digital",
                "Lakukan proses audit dan pengawasan lahan secara digital, terstruktur, dan efisien langsung dari perangkat Anda."
            ),
            TutorialItem(
                R.drawable.ic_map,
                "Pemetaan Poligon",
                "Pantau lokasi dan poligon batas lahan sawit secara real-time melalui integrasi peta digital interaktif."
            )
        )

        tutorialAdapter = TutorialAdapter(tutorialList)
        viewPager.adapter = tutorialAdapter

        // Sync TabLayout with ViewPager2 for dot indicator
        TabLayoutMediator(tabLayout, viewPager) { _, _ -> }.attach()

        // Handle Next / Start Button
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == tutorialList.size - 1) {
                    btnNext.text = "Mulai"
                } else {
                    btnNext.text = "Selanjutnya"
                }
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem + 1 < tutorialAdapter.itemCount) {
                viewPager.currentItem += 1
            } else {
                // Selesai tutorial
                val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putBoolean("is_first_time", false)
                editor.apply()

                // Lanjut ke halaman Masuk
                val intent = Intent(this, MasukActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}
