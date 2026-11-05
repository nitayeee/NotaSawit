package com.example.notasawit.Utils

import android.app.Activity

object StatusSplashHelper {

    fun showSuccess(
        activity: Activity,
        title: String = "Berhasil Disimpan!",
        message: String = "",
        category: String = "",
        finishCaller: Boolean = true
    ) {
        val alertTitle = if (title.contains("Berhasil", ignoreCase = true)) "Berhasil" else title
        val alertMessage = when {
            message.isNotEmpty() -> message
            title.isNotEmpty() -> title
            else -> "Data berhasil disimpan"
        }
        CustomAlert.setPendingSuccess(activity, alertTitle, alertMessage)
        if (finishCaller) {
            activity.finish()
        }
    }

    fun showError(
        activity: Activity,
        title: String = "Peringatan",
        message: String = "",
        category: String = ""
    ) {
        val alertTitle = when {
            title.contains("Gagal", ignoreCase = true) -> "Gagal"
            title.contains("Peringatan", ignoreCase = true) -> "Peringatan"
            else -> "Peringatan"
        }
        val alertMsg = if (message.isNotEmpty()) message else title
        CustomAlert.showError(activity, alertTitle, alertMsg)
    }
}
