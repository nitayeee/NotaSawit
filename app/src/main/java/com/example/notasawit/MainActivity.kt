package com.example.notasawit

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.DecelerateInterpolator
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.notasawit.Autentikasi.Masuk.MasukActivity
import com.example.notasawit.databinding.ActivityMainBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.hypot

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

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

        lifecycleScope.launch {
            runSplashSequence()
        }
    }

    private suspend fun runSplashSequence() {
        // Initial setup (SplashScreen 1): Dark green background with LARGE white logo
        binding.viewGreenBg.visibility = View.VISIBLE
        binding.ivLogoWhite.apply {
            alpha = 1f
            scaleX = 1.5f
            scaleY = 1.5f
        }
        binding.ivLogoColored.apply {
            alpha = 0f
            scaleX = 1.0f
            scaleY = 1.0f
        }
        binding.tvBrandName.apply {
            alpha = 0f
            visibility = View.GONE
        }
        binding.tvTagline.apply {
            alpha = 0f
            visibility = View.GONE
        }

        // ==========================================
        // 1. SplashScreen 1: Display large white logo on dark green bg
        // Wait 800ms
        // ==========================================
        delay(800)

        // ==========================================
        // 2. SplashScreen 1 -> SplashScreen 3
        // Green background shrinks as a circle into TOP-RIGHT corner (ViewAnimationUtils)
        // White logo scales down (1.5f -> 1.0f) and cross-fades into Colored logo
        // ==========================================
        val greenView = binding.viewGreenBg
        val cx = greenView.width
        val cy = 0
        val initialRadius = hypot(greenView.width.toDouble(), greenView.height.toDouble()).toFloat()

        val circularAnim = ViewAnimationUtils.createCircularReveal(
            greenView,
            cx,
            cy,
            initialRadius,
            0f
        ).apply {
            duration = 600
            interpolator = DecelerateInterpolator()
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    greenView.visibility = View.GONE
                }
            })
        }

        val animLogoWhiteAlpha = ObjectAnimator.ofFloat(binding.ivLogoWhite, View.ALPHA, 1f, 0f)
        val animLogoWhiteScaleX = ObjectAnimator.ofFloat(binding.ivLogoWhite, View.SCALE_X, 1.5f, 1f)
        val animLogoWhiteScaleY = ObjectAnimator.ofFloat(binding.ivLogoWhite, View.SCALE_Y, 1.5f, 1f)
        val animLogoColorAlpha = ObjectAnimator.ofFloat(binding.ivLogoColored, View.ALPHA, 0f, 1f)

        circularAnim.start()
        AnimatorSet().apply {
            playTogether(
                animLogoWhiteAlpha,
                animLogoWhiteScaleX,
                animLogoWhiteScaleY,
                animLogoColorAlpha
            )
            duration = 600
            interpolator = DecelerateInterpolator()
            start()
        }

        // ==========================================
        // 3. SplashScreen 3 -> SplashScreen 4
        // Result: Colored Logo shifts left + SILAUSA text (Gradient) + Subtitle Tagline fade in
        // ==========================================
        delay(800)

        applyTextGradient()

        TransitionManager.beginDelayedTransition(
            binding.llCenterWrapper,
            AutoTransition().apply {
                duration = 300
                interpolator = DecelerateInterpolator()
            }
        )

        binding.tvBrandName.visibility = View.VISIBLE
        binding.tvTagline.visibility = View.VISIBLE

        val animBrand = ObjectAnimator.ofFloat(binding.tvBrandName, View.ALPHA, 0f, 1f)
        val animTagline = ObjectAnimator.ofFloat(binding.tvTagline, View.ALPHA, 0f, 1f)

        AnimatorSet().apply {
            playTogether(animBrand, animTagline)
            duration = 350
            interpolator = DecelerateInterpolator()
            start()
        }

        // ==========================================
        // 4. SplashScreen 4 -> Hold 2 seconds -> Navigate
        // ==========================================
        delay(2000)

        navigateToNextScreen()
    }

    private fun applyTextGradient() {
        binding.tvBrandName.post {
            val paint = binding.tvBrandName.paint
            val width = paint.measureText(binding.tvBrandName.text.toString())
            if (width > 0f) {
                val textShader = LinearGradient(
                    0f, 0f, width, 0f,
                    intArrayOf(
                        Color.parseColor("#002617"), // dark green (SI)
                        Color.parseColor("#1B4D2E"), // primary green (LAU)
                        Color.parseColor("#2E7D32")  // accent green (SA)
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
                paint.shader = textShader
                binding.tvBrandName.invalidate()
            }
        }
    }

    private fun navigateToNextScreen() {
        if (isFinishing || isDestroyed) return

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