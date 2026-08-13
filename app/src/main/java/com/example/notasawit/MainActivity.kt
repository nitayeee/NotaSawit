package com.example.notasawit

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Autentikasi.Masuk.MasukActivity
import com.example.notasawit.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val loopingAnimators = mutableListOf<ValueAnimator>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startAmbientBlobs()
        startDotsBounce(listOf(binding.viewDot1, binding.viewDot2, binding.viewDot3))

        lifecycleScope.launch {
            runEntranceSequence()

            delay(1000) // simulasi loading / jeda

            stopLoopingAnimators()

            val sharedPref = getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
            val isFirstTime = sharedPref.getBoolean("is_first_time", true)
            val role = sharedPref.getString("role", "")

            val intent = if (isFirstTime) {
                Intent(this@MainActivity, TutorialActivity::class.java)
            } else {
                Intent(this@MainActivity, MasukActivity::class.java).apply {
                    if (role == "petani" || role == "admin") {
                        putExtra("SHOW_FINGERPRINT", true)
                    }
                }
            }
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }
    }

    private suspend fun runEntranceSequence() {
        val logoWrapper = binding.flLogoWrapper
        val brandTitle = binding.tvBrandTitle
        val bottomInfo = binding.llBottomInfo
        val shine = binding.viewShine
        val borderGlow = binding.viewBorderGlow

        logoWrapper.apply { alpha = 0f; scaleX = 0.4f; scaleY = 0.4f }
        brandTitle.text = ""
        bottomInfo.apply { alpha = 0f }
        shine.alpha = 0f

        // 1. Logo Appears
        ObjectAnimator.ofFloat(logoWrapper, View.ALPHA, 0f, 1f).apply { duration = 650 }.start()
        ObjectAnimator.ofFloat(logoWrapper, View.SCALE_X, 0.4f, 1f).apply {
            duration = 650
            interpolator = OvershootInterpolator(2f)
        }.start()
        ObjectAnimator.ofFloat(logoWrapper, View.SCALE_Y, 0.4f, 1f).apply {
            duration = 650
            interpolator = OvershootInterpolator(2f)
        }.start()

        delay(650)
        startPulseLoop(logoWrapper)

        // 2. Animate brand text letter by letter
        val text = "SILAUSA"
        var currentText = ""
        for (i in text.indices) {
            currentText += text[i]
            val spannable = SpannableString(currentText)
            if (currentText.length > 2) {
                spannable.setSpan(
                    ForegroundColorSpan(Color.parseColor("#002617")),
                    0, 2,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannable.setSpan(
                    ForegroundColorSpan(Color.parseColor("#1B4D2E")),
                    2, currentText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            } else {
                spannable.setSpan(
                    ForegroundColorSpan(Color.parseColor("#002617")),
                    0, currentText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            brandTitle.text = spannable
            delay(150)
        }

        // 3. Version text appears
        ObjectAnimator.ofFloat(bottomInfo, View.ALPHA, 0f, 1f).apply { duration = 400 }.start()
        delay(400)

        // 4. Shine on logo
        ObjectAnimator.ofFloat(shine, View.ALPHA, 0f, 1f, 0f).apply { duration = 550 }.start()
        ObjectAnimator.ofFloat(shine, View.TRANSLATION_X, -180f, 180f).apply { duration = 550 }.start()
        delay(550)

        // 5. Glow / Vignette appear
        ObjectAnimator.ofFloat(borderGlow, View.ALPHA, 0f, 1f).apply { duration = 700 }.start()
        delay(700)
    }

    private fun startAmbientBlobs() {
        floatLoop(binding.viewBlobTop, amplitude = 18f, duration = 4200, delay = 0)
        floatLoop(binding.viewBlobBottom, amplitude = 24f, duration = 5000, delay = 300)
    }

    private fun floatLoop(target: View, amplitude: Float, duration: Long, delay: Long) {
        val animator = ValueAnimator.ofFloat(-amplitude, amplitude).apply {
            this.duration = duration
            startDelay = delay
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { target.translationY = it.animatedValue as Float }
            start()
        }
        loopingAnimators.add(animator)
    }

    private fun startPulseLoop(target: View) {
        val animator = ValueAnimator.ofFloat(1f, 1.06f, 1f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val scale = it.animatedValue as Float
                target.scaleX = scale
                target.scaleY = scale
            }
            start()
        }
        loopingAnimators.add(animator)
    }

    private fun startDotsBounce(dots: List<View>) {
        dots.forEachIndexed { index, dot ->
            val animator = ValueAnimator.ofFloat(0f, -6f).apply {
                duration = 400
                startDelay = index * 150L
                repeatMode = ValueAnimator.REVERSE
                repeatCount = ValueAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { dot.translationY = it.animatedValue as Float }
                start()
            }
            loopingAnimators.add(animator)
        }
    }

    private fun stopLoopingAnimators() {
        loopingAnimators.forEach { it.cancel() }
        loopingAnimators.clear()
    }

    override fun onDestroy() {
        stopLoopingAnimators()
        super.onDestroy()
    }
}