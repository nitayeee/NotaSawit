package com.example.notasawit

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
        runEntranceAnimation()

        lifecycleScope.launch {
            delay(2000) // simulasi pengambilan data selama 2 detik

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

    private fun runEntranceAnimation() {
        val logoWrapper = binding.flLogoWrapper
        val brandText = binding.llBrandText
        val versionInfo = binding.llBottomInfo
        val shine = binding.viewShine

        logoWrapper.apply { alpha = 0f; scaleX = 0.4f; scaleY = 0.4f }
        brandText.apply { alpha = 0f; translationY = 60f }
        versionInfo.apply { alpha = 0f }
        shine.alpha = 0f

        val logoSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(logoWrapper, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(logoWrapper, View.SCALE_X, 0.4f, 1f),
                ObjectAnimator.ofFloat(logoWrapper, View.SCALE_Y, 0.4f, 1f)
            )
            duration = 650
            interpolator = OvershootInterpolator(2f)
        }

        val textSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(brandText, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(brandText, View.TRANSLATION_Y, 60f, 0f)
            )
            duration = 500
            interpolator = AccelerateDecelerateInterpolator()
            startDelay = 300
        }

        val infoFade = ObjectAnimator.ofFloat(versionInfo, View.ALPHA, 0f, 1f).apply {
            duration = 400
            startDelay = 700
        }

        // sapuan "shine" satu kali di atas logo, setelah logo pop selesai
        val shineSet = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(shine, View.ALPHA, 0f, 1f, 0f).apply { duration = 550 },
                ObjectAnimator.ofFloat(shine, View.TRANSLATION_X, -180f, 180f).apply { duration = 550 }
            )
            startDelay = 600
        }

        AnimatorSet().apply {
            playTogether(logoSet, textSet, infoFade, shineSet)
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    startPulseLoop(logoWrapper)
                    startDotsBounce(listOf(binding.viewDot1, binding.viewDot2, binding.viewDot3))
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
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