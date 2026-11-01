package com.example.notasawit.Utils

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.view.Window
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
            setDimAmount(0.6f) // Membuat homepage di belakang menggelap seperti popup
        }

        val tvGreetingTitle = dialog.findViewById<TextView>(R.id.tvGreetingTitle)
        val ivMascot = dialog.findViewById<ImageView>(R.id.ivMascot)
        val dialogRoot = dialog.findViewById<ViewGroup>(R.id.dialogRoot)

        tvGreetingTitle.text = "Halo, $userName! 👋"

        // Load animasi karakter sawit melambai tanpa background (transparan)
        Glide.with(activity.applicationContext)
            .asDrawable()
            .load(R.drawable.sawit_wave_transparent)
            .into(ivMascot)

        // Efek animasi muncul halus
        ivMascot.scaleX = 0.85f
        ivMascot.scaleY = 0.85f
        ivMascot.alpha = 0f
        ivMascot.animate()
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

            dialogRoot?.animate()
                ?.alpha(0f)
                ?.setDuration(250)
                ?.withEndAction {
                    try {
                        if (!activity.isFinishing && !activity.isDestroyed && dialog.isShowing) {
                            dialog.dismiss()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                ?.start()
        }

        // Ketuk di mana saja pada layar untuk langsung menutup dan mulai beraktivitas
        dialogRoot?.setOnClickListener {
            dismissWithAnimation()
        }

        dialog.setOnCancelListener {
            dismissWithAnimation()
        }

        // Otomatis menutup setelah melambai sekitar 2.8 detik
        handler.postDelayed({
            dismissWithAnimation()
        }, 2800)

        try {
            dialog.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

