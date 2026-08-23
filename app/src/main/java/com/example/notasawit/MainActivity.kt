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
        // Initial setup: Dark green background with white logo initially invisible
        binding.viewGreenBg.visibility = View.VISIBLE
        binding.ivLogoWhite.apply {
            alpha = 0f
            scaleX = 0.85f
            scaleY = 0.85f
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

        delay(100)

        // 1. Initial State: White logo animates in smoothly (fade in + gentle scale up)
        val smoothInterpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()

        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.ivLogoWhite, View.ALPHA, 0f, 1f),
                ObjectAnimator.ofFloat(binding.ivLogoWhite, View.SCALE_X, 0.85f, 1f),
                ObjectAnimator.ofFloat(binding.ivLogoWhite, View.SCALE_Y, 0.85f, 1f)
            )
            duration = 550
            interpolator = smoothInterpolator
            start()
        }

        delay(750)

        // 2. Green circular background shrinks towards TOP-RIGHT corner (ultra smooth)
        // Simultaneously, white logo cross-fades into colored logo at exact center with matching 1.0f scale
        val greenView = binding.viewGreenBg
        val width = if (greenView.width > 0) greenView.width else resources.displayMetrics.widthPixels
        val height = if (greenView.height > 0) greenView.height else resources.displayMetrics.heightPixels

        val cx = width
        val cy = 0
        val initialRadius = hypot(width.toDouble(), height.toDouble()).toFloat()

        greenView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        val circularAnim = ViewAnimationUtils.createCircularReveal(
            greenView,
            cx,
            cy,
            initialRadius,
            0f
        ).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    greenView.visibility = View.GONE
                    greenView.setLayerType(View.LAYER_TYPE_NONE, null)
                }
            })
        }

        val animLogoWhiteAlpha = ObjectAnimator.ofFloat(binding.ivLogoWhite, View.ALPHA, 1f, 0f)
        val animLogoColorAlpha = ObjectAnimator.ofFloat(binding.ivLogoColored, View.ALPHA, 0f, 1f)

        AnimatorSet().apply {
            playTogether(
                circularAnim,
                animLogoWhiteAlpha,
                animLogoColorAlpha
            )
            duration = 650
            interpolator = smoothInterpolator
            start()
        }

        delay(650)

        // 3. Colored logo shifts left as SILAUSA brand text appears on white background
        applyTextGradient()

        TransitionManager.beginDelayedTransition(
            binding.llCenterWrapper,
            AutoTransition().apply {
                duration = 400
                interpolator = DecelerateInterpolator()
            }
        )

        binding.tvBrandName.visibility = View.VISIBLE

        ObjectAnimator.ofFloat(binding.tvBrandName, View.ALPHA, 0f, 1f).apply {
            duration = 400
            interpolator = DecelerateInterpolator()
            start()
        }

        // 4. Hold & Navigate
        delay(1800)

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