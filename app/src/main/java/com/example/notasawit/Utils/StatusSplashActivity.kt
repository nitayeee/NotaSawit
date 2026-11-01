package com.example.notasawit.Utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        val title = intent.getStringExtra(EXTRA_TITLE) ?: if (status == STATUS_SUCCESS) "Data Berhasil Disimpan!" else "Input Data Gagal!"
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: "Data Sawit"

        binding.tvStatusTitle.text = title
        binding.tvStatusMessage.text = message
        binding.tvCategoryBadge.text = category

        // Efek animasi masuk badge
        binding.badgeContainer.scaleX = 0.8f
        binding.badgeContainer.scaleY = 0.8f
        binding.badgeContainer.alpha = 0f
        binding.badgeContainer.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(400)
            .setInterpolator(DecelerateInterpolator())
            .start()

        if (status == STATUS_SUCCESS) {
            setupSuccessUI()
        } else {
            setupFailedUI()
        }

        binding.btnStatusAction.setOnClickListener {
            finishWithFade()
        }
    }

    private fun setupSuccessUI() {
        val greenColor = Color.parseColor("#1B4D2E")
        val softGreen = Color.parseColor("#E8ECE9")

        binding.tvStatusTitle.setTextColor(greenColor)
        binding.badgeStatus.setBackgroundResource(R.drawable.bg_circle_success)
        binding.ivStatusIcon.setImageResource(R.drawable.ic_check)
        binding.ivStatusIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)

        binding.tvCategoryBadge.backgroundTintList = ColorStateList.valueOf(softGreen)
        binding.tvCategoryBadge.setTextColor(greenColor)

        binding.btnStatusAction.text = "Selesai"
        binding.btnStatusAction.backgroundTintList = ColorStateList.valueOf(greenColor)

        // Progress bar otomatis berjalan 2.2 detik lalu menutup sendiri
        binding.progressAutoClose.visibility = View.VISIBLE
        binding.progressAutoClose.progress = 0
        progressAnimator = ObjectAnimator.ofInt(binding.progressAutoClose, "progress", 0, 100).apply {
            duration = 2200
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    finishWithFade()
                }
            })
            start()
        }
    }

    private fun setupFailedUI() {
        val redColor = Color.parseColor("#D32F2F")
        val softRed = Color.parseColor("#FFEBEE")

        binding.tvStatusTitle.setTextColor(redColor)
        binding.badgeStatus.setBackgroundResource(R.drawable.bg_circle_error)
        binding.ivStatusIcon.setImageResource(R.drawable.ic_close)
        binding.ivStatusIcon.imageTintList = ColorStateList.valueOf(Color.WHITE)

        binding.tvCategoryBadge.backgroundTintList = ColorStateList.valueOf(softRed)
        binding.tvCategoryBadge.setTextColor(redColor)

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

