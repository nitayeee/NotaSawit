package com.example.notasawit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.notasawit.Autentikasi.Masuk.MasukActivity
import com.google.android.material.button.MaterialButton

class TutorialActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: MaterialButton
    private lateinit var tvSkip: TextView
    private lateinit var tutorialAdapter: TutorialAdapter
    private lateinit var dots: List<View>

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
        btnNext = findViewById(R.id.btnNext)
        tvSkip = findViewById(R.id.tvSkip)
        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3)
        )

        val tutorialList = listOf(
            TutorialItem(
                R.drawable.onboarding_1,
                "Selamat Datang di SILAUSA",
                "Pencatatan dan pengelolaan data kebun sawit menjadi lebih mudah dan terpusat dalam satu aplikasi."
            ),
            TutorialItem(
                R.drawable.onboarding_2,
                "Audit Internal Digital",
                "Lakukan proses audit dan pengawasan lahan secara digital, terstruktur, dan efisien langsung dari perangkat Anda."
            ),
            TutorialItem(
                R.drawable.onboarding_3,
                "Pemetaan Poligon",
                "Pantau lokasi dan poligon batas lahan sawit secara real-time melalui integrasi peta digital interaktif."
            )
        )

        tutorialAdapter = TutorialAdapter(tutorialList)
        viewPager.adapter = tutorialAdapter

        updateDots(0)
        updateButtonState(0, tutorialList.size)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
                updateButtonState(position, tutorialList.size)
            }
        })

        tvSkip.setOnClickListener {
            finishOnboarding()
        }

        btnNext.setOnClickListener {
            if (viewPager.currentItem + 1 < tutorialAdapter.itemCount) {
                viewPager.currentItem += 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun updateButtonState(position: Int, totalItems: Int) {
        val isLastPage = position == totalItems - 1
        if (isLastPage) {
            btnNext.text = "Mulai"
            val params = btnNext.layoutParams
            params.width = (110 * resources.displayMetrics.density).toInt()
            btnNext.layoutParams = params
        } else {
            btnNext.text = "→"
            val params = btnNext.layoutParams
            params.width = (56 * resources.displayMetrics.density).toInt()
            btnNext.layoutParams = params
        }
    }

    private fun updateDots(position: Int) {
        val activeWidth = (22 * resources.displayMetrics.density).toInt()
        val inactiveWidth = (8 * resources.displayMetrics.density).toInt()

        dots.forEachIndexed { index, dot ->
            val params = dot.layoutParams
            if (index == position) {
                params.width = activeWidth
                dot.setBackgroundResource(R.drawable.bg_dot_active)
            } else {
                params.width = inactiveWidth
                dot.setBackgroundResource(R.drawable.bg_dot_inactive)
            }
            dot.layoutParams = params
        }
    }

    private fun finishOnboarding() {
        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("is_first_time", false)
        editor.apply()

        val intent = Intent(this, MasukActivity::class.java)
        startActivity(intent)
        finish()
    }
}
