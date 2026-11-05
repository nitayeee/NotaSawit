package com.example.notasawit.Utils

import android.app.Activity

object WelcomeMascotDialog {

    fun show(activity: Activity, userName: String) {
        CustomAlert.showSuccess(activity, "Berhasil", "Selamat datang, $userName")
    }
}
