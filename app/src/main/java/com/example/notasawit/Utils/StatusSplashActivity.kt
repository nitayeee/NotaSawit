package com.example.notasawit.Utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.notasawit.R
import com.example.notasawit.databinding.ActivityStatusSplashBinding

class StatusSplashActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_STATUS = "EXTRA_STATUS"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_MESSAGE = "EXTRA_MESSAGE"
        const val EXTRA_CATEGORY = "EXTRA_CATEGORY"

        const val STATUS_SUCCESS = "SUCCESS"
        const val STATUS_FAILED = "FAILED"
    }

    private lateinit var binding: ActivityStatusSplashBinding
    private var progressAnimator: ObjectAnimator? = null
    private var isFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityStatusSplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val status = intent.getStringExtra(EXTRA_STATUS) ?: STATUS_SUCCESS
        val title = intent.getStringExtra(EXTRA_TITLE)
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: ""

        if (status == STATUS_SUCCESS) {
            setupSuccessUI(title)
        } else {
            setupFailedUI(title, message, category)
        }

        binding.btnStatusAction.setOnClickListener {
            finishWithFade()
        }

        binding.layoutRoot.setOnClickListener {
            if (status == STATUS_SUCCESS) {
                finishWithFade()
            }
        }
    }

    private fun setupSuccessUI(customTitle: String?) {
        val greenColor = Color.parseColor("#1B4D2E")

        binding.cardVideoSuccess.visibility = View.VISIBLE
        binding.badgeErrorContainer.visibility = View.GONE

        // Load animasi video sawit melompat gembira & memberi jempol HD
        Glide.with(applicationContext)
            .asDrawable()
            .load(R.drawable.sawit_success_anim)
            .into(binding.ivSuccessVideo)

        // Efek animasi card video muncul membesar lembut
        binding.cardVideoSuccess.scaleX = 0.9f
        binding.cardVideoSuccess.scaleY = 0.9f
        binding.cardVideoSuccess.alpha = 0f
        binding.cardVideoSuccess.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Sesuai permintaan pengguna: "Bikin aja berhasil disimpan gitu. Jangan banyak kali tulisan, bikin jelek"
        binding.tvStatusTitle.text = customTitle?.ifEmpty { "Berhasil Disimpan!" } ?: "Berhasil Disimpan!"
        binding.tvStatusTitle.setTextColor(greenColor)

        // Sembunyikan pesan penjelas panjang dan chip agar tampilan bersih dan elegan
        binding.tvCategoryBadge.visibility = View.GONE
        binding.tvStatusMessage.visibility = View.GONE

        binding.btnStatusAction.text = "Selesai"
        binding.btnStatusAction.backgroundTintList = ColorStateList.valueOf(greenColor)

        // Progress bar otomatis berjalan ~3.8 detik lalu menutup sendiri
        binding.progressAutoClose.visibility = View.VISIBLE
        binding.progressAutoClose.progress = 0
        progressAnimator = ObjectAnimator.ofInt(binding.progressAutoClose, "progress", 0, 100).apply {
            duration = 3800
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishWithFade()
                }
            })
            start()
        }
    }

    private fun setupFailedUI(customTitle: String?, message: String, category: String) {
        val redColor = Color.parseColor("#D32F2F")

        binding.cardVideoSuccess.visibility = View.GONE
        binding.badgeErrorContainer.visibility = View.VISIBLE

        binding.badgeErrorContainer.scaleX = 0.85f
        binding.badgeErrorContainer.scaleY = 0.85f
        binding.badgeErrorContainer.alpha = 0f
        binding.badgeErrorContainer.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .start()

        binding.tvStatusTitle.text = customTitle?.ifEmpty { "Input Data Gagal!" } ?: "Input Data Gagal!"
        binding.tvStatusTitle.setTextColor(redColor)

        if (message.isNotEmpty()) {
            binding.tvStatusMessage.text = message
            binding.tvStatusMessage.visibility = View.VISIBLE
        } else {
            binding.tvStatusMessage.visibility = View.GONE
        }

        binding.tvCategoryBadge.visibility = View.GONE

        binding.btnStatusAction.text = "Periksa Kembali"
        binding.btnStatusAction.backgroundTintList = ColorStateList.valueOf(redColor)

        binding.progressAutoClose.visibility = View.GONE
    }

    private fun finishWithFade() {
        if (isFinished) return
        isFinished = true
        progressAnimator?.cancel()
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        progressAnimator?.cancel()
    }
}
