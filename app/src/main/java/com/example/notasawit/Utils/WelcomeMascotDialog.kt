package com.example.notasawit.Utils

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.notasawit.R

object WelcomeMascotDialog {

    fun show(activity: Activity, userName: String) {
        if (activity.isFinishing || activity.isDestroyed) return

        val dialog = Dialog(activity)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_welcome_mascot)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setDimAmount(0.6f) // Membuat tampilan homepage di latar belakang menggelap elegan
        }

        val tvGreetingTitle = dialog.findViewById<TextView>(R.id.tvGreetingTitle)
        val ivMascot = dialog.findViewById<ImageView>(R.id.ivMascot)
        val cardPopup = dialog.findViewById<View>(R.id.cardPopup)
        val btnMasuk = dialog.findViewById<Button>(R.id.btnMasuk)
        val dialogRoot = dialog.findViewById<ViewGroup>(R.id.dialogRoot)

        tvGreetingTitle.text = "Halo, $userName!"

        // Load animasi karakter sawit melambai HD dalam card
        Glide.with(activity.applicationContext)
            .asDrawable()
            .load(R.drawable.sawit_wave_card)
            .into(ivMascot)

        // Efek animasi card muncul lembut
        cardPopup.scaleX = 0.85f
        cardPopup.scaleY = 0.85f
        cardPopup.alpha = 0f
        cardPopup.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(350)
            .start()

        val handler = Handler(Looper.getMainLooper())
        var isDismissed = false

        fun dismissWithAnimation() {
            if (isDismissed) return
            isDismissed = true
            handler.removeCallbacksAndMessages(null)

            cardPopup.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(220)
                .withEndAction {
                    try {
                        if (!activity.isFinishing && !activity.isDestroyed && dialog.isShowing) {
                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                .start()
        }

        // Tombol masuk ke beranda
        btnMasuk.setOnClickListener {
            dismissWithAnimation()
        }

        // Ketuk di luar card untuk langsung menutup
        dialogRoot.setOnClickListener {
            dismissWithAnimation()
        }

        dialog.setOnCancelListener {
            dismissWithAnimation()
        }

        // Otomatis menutup setelah melambai sekitar 3 detik
        handler.postDelayed({
            dismissWithAnimation()
        }, 3000)

        try {
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
