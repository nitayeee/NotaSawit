package com.example.notasawit.Utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.notasawit.R

object CustomAlert {

    private const val PREF_ALERT = "NOTASAWIT_ALERT_PREF"
    private const val KEY_PENDING_TITLE = "pending_title"
    private const val KEY_PENDING_MESSAGE = "pending_message"

    fun setPendingSuccess(context: android.content.Context, title: String, message: String) {
        val pref = context.getSharedPreferences(PREF_ALERT, android.content.Context.MODE_PRIVATE)
        pref.edit()
            .putString(KEY_PENDING_TITLE, title)
            .putString(KEY_PENDING_MESSAGE, message)
            .apply()
    }

    fun checkAndShowPendingAlert(activity: Activity) {
        val pref = activity.getSharedPreferences(PREF_ALERT, android.content.Context.MODE_PRIVATE)
        val title = pref.getString(KEY_PENDING_TITLE, null)
        val message = pref.getString(KEY_PENDING_MESSAGE, null)
        if (!title.isNullOrEmpty() && !message.isNullOrEmpty()) {
            pref.edit().clear().apply()
            activity.window?.decorView?.postDelayed({
                if (!activity.isFinishing && !activity.isDestroyed) {
                    showSuccess(activity, title, message)
                }
            }, 300)
        }
    }

    fun showSuccess(activity: Activity, title: String, message: String) {
        showAlert(activity, title, message, true)
    }

    fun showError(activity: Activity, title: String, message: String) {
        showAlert(activity, title, message, false)
    }

    private fun showAlert(activity: Activity, title: String, message: String, isSuccess: Boolean) {
        val rootView = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        
        // Cek jika sudah ada alert, hapus dulu agar tidak tumpuk
        val existingAlert = rootView.findViewWithTag<View>("custom_alert_tag")
        if (existingAlert != null) {
            rootView.removeView(existingAlert)
        }

        val inflater = LayoutInflater.from(activity)
        val alertView = inflater.inflate(R.layout.layout_custom_alert, rootView, false)
        alertView.tag = "custom_alert_tag"

        val container = alertView.findViewById<LinearLayout>(R.id.alertContainer)
        val tvTitle = alertView.findViewById<TextView>(R.id.tvAlertTitle)
        val tvMessage = alertView.findViewById<TextView>(R.id.tvAlertMessage)

        tvTitle.text = title
        tvMessage.text = message

        if (isSuccess) {
            container.setBackgroundResource(R.drawable.bg_alert_success)
        } else {
            container.setBackgroundResource(R.drawable.bg_alert_error)
        }

        val layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        )
        // Set ke atas
        layoutParams.gravity = android.view.Gravity.TOP
        alertView.layoutParams = layoutParams

        rootView.addView(alertView)

        // Pastikan view berada paling atas
        alertView.elevation = 100f
        alertView.translationZ = 100f

        // Ukur view untuk mendapatkan tinggi yang akurat sebelum layout selesai
        val screenWidth = if (rootView.width > 0) rootView.width else activity.resources.displayMetrics.widthPixels
        alertView.measure(
            View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val measuredH = alertView.measuredHeight.toFloat()
        val alertHeight = if (measuredH > 0f) measuredH else (120f * activity.resources.displayMetrics.density)

        // Set posisi awal di luar layar atas
        alertView.translationY = -alertHeight

        // Animasi masuk (slide down)
        val animator = ObjectAnimator.ofFloat(alertView, View.TRANSLATION_Y, 0f)
        animator.duration = 400
        animator.interpolator = DecelerateInterpolator()
        animator.start()

        // Hilang otomatis setelah 3 detik
        Handler(Looper.getMainLooper()).postDelayed({
            if (alertView.parent != null) {
                val outHeight = if (alertView.height > 0) alertView.height.toFloat() else alertView.measuredHeight.toFloat()
                val animatorOut = ObjectAnimator.ofFloat(alertView, View.TRANSLATION_Y, -outHeight)
                animatorOut.duration = 400
                animatorOut.interpolator = DecelerateInterpolator()
                animatorOut.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        rootView.removeView(alertView)
                    }
                })
                animatorOut.start()
            }
        }, 3000)
    }

    fun showImagePreview(context: android.content.Context, imageUrl: String?) {
        if (imageUrl.isNullOrEmpty()) return
        val dialog = android.app.Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)

        val layout = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.parseColor("#E6000000"))
        }

        val imageView = ImageView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                gravity = android.view.Gravity.CENTER
            }
            scaleType = ImageView.ScaleType.FIT_CENTER
            val density = context.resources.displayMetrics.density
            val pad = (24 * density).toInt()
            setPadding(pad, pad * 2, pad, pad * 2)
        }

        com.bumptech.glide.Glide.with(context)
            .load(imageUrl)
            .into(imageView)

        val closeBtn = ImageView(context).apply {
            val density = context.resources.displayMetrics.density
            val size = (44 * density).toInt()
            val margin = (24 * density).toInt()
            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                topMargin = margin
                marginEnd = margin
            }
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(android.graphics.Color.WHITE)
            setBackgroundResource(R.drawable.bg_toolbar_btn)
            val pad = (10 * density).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = 10f
            setOnClickListener {
                dialog.dismiss()
            }
        }

        layout.addView(imageView)
        layout.addView(closeBtn)

        layout.setOnClickListener {
            dialog.dismiss()
        }

        dialog.setContentView(layout)
        dialog.show()
    }
}
