package com.example.notasawit.Autentikasi.Masuk

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.notasawit.BaseActivity
import com.example.notasawit.Admin.BaseAdminActivity
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private var progressAnimator: ObjectAnimator? = null
    private var isNavigated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val userName = intent.getStringExtra("EXTRA_NAME")
            ?: sharedPref.getString("namaPetani", null)
            ?: sharedPref.getString("username", "Petani Sawit")

        val userRole = intent.getStringExtra("EXTRA_ROLE")
            ?: sharedPref.getString("role", "petani")

        binding.tvUserName.text = "Halo, $userName!"

        // Load animasi karakter sawit melambai (animasi WebP) dengan Glide
        Glide.with(this)
            .asDrawable()
            .load(R.drawable.sawit_wave)
            .placeholder(R.drawable.sawit_mascot)
            .into(binding.ivMascot)

        // Animasi progress bar bergerak halus dari 0% ke 100% untuk indikasi pindah ke homepage
        binding.progressWelcome.progress = 0
        progressAnimator = ObjectAnimator.ofInt(binding.progressWelcome, "progress", 0, 100).apply {
            duration = 2800 // 2.8 detik melambai lalu pindah ke homepage
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    lanjutKeHalamanUtama(userRole)
                }
            })
            start()
        }

        // Ketuk layar atau tombol lewati untuk langsung masuk tanpa menunggu
        binding.layoutRoot.setOnClickListener {
            lanjutKeHalamanUtama(userRole)
        }
        binding.btnSkip.setOnClickListener {
            lanjutKeHalamanUtama(userRole)
        }
    }

    private fun lanjutKeHalamanUtama(role: String?) {
        if (isNavigated) return
        isNavigated = true
        progressAnimator?.cancel()

        val targetIntent = if (role == "admin") {
            Intent(this, BaseAdminActivity::class.java)
        } else {
            Intent(this, BaseActivity::class.java)
        }

        targetIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(targetIntent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        progressAnimator?.cancel()
    }
}
