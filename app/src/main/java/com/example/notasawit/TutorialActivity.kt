package com.example.notasawit

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager2.widget.ViewPager2
import com.example.notasawit.Autentikasi.Masuk.MasukActivity

class TutorialActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var btnNext: Button
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
        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3)
        )

        val tutorialList = listOf(
            TutorialItem(
                R.drawable.logo,
                "Selamat Datang di Notasawit",
                "Pencatatan dan pengelolaan data kebun sawit menjadi lebih mudah dan terpusat dalam satu aplikasi."
            ),
            TutorialItem(
                R.drawable.audit_onboarding,
                "Audit Internal Digital",
                "Lakukan proses audit dan pengawasan lahan secara digital, terstruktur, dan efisien langsung dari perangkat Anda."
            ),
            TutorialItem(
                R.drawable.pemetaan_onboarding,
                "Pemetaan Poligon",
                "Pantau lokasi dan poligon batas lahan sawit secara real-time melalui integrasi peta digital interaktif."
            )
        )
        

        tutorialAdapter = TutorialAdapter(tutorialList)
        viewPager.adapter = tutorialAdapter

        updateDots(0)

        // Handle Page Change & Dot Indicators
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateDots(position)
                if (position == tutorialList.size - 1) {
                    btnNext.visibility = View.VISIBLE
                    btnNext.alpha = 0f
                    btnNext.animate().alpha(1f).setDuration(250).start()
                } else {
                    btnNext.visibility = View.GONE
                }
            }
        })

        btnNext.setOnClickListener {
            if (viewPager.currentItem + 1 < tutorialAdapter.itemCount) {
                viewPager.currentItem += 1
            } else {
                val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
                val editor = sharedPref.edit()
                editor.putBoolean("is_first_time", false)
                editor.apply()

                val intent = Intent(this, MasukActivity::class.java)
                startActivity(intent)
                finish()
            }
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
}
