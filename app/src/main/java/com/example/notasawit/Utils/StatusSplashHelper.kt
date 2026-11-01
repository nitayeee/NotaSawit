package com.example.notasawit.Utils

import android.app.Activity
import android.content.Intent

object StatusSplashHelper {

    fun showSuccess(
        activity: Activity,
        title: String,
        message: String,
        category: String,
        finishCaller: Boolean = true
    ) {
        val intent = Intent(activity, StatusSplashActivity::class.java).apply {
            putExtra(StatusSplashActivity.EXTRA_STATUS, StatusSplashActivity.STATUS_SUCCESS)
            putExtra(StatusSplashActivity.EXTRA_TITLE, title)
            putExtra(StatusSplashActivity.EXTRA_MESSAGE, message)
            putExtra(StatusSplashActivity.EXTRA_CATEGORY, category)
        }
        activity.startActivity(intent)
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        if (finishCaller) {
            activity.finish()
        }
    }

    fun showError(
        activity: Activity,
        title: String,
        message: String,
        category: String
    ) {
        val intent = Intent(activity, StatusSplashActivity::class.java).apply {
            putExtra(StatusSplashActivity.EXTRA_STATUS, StatusSplashActivity.STATUS_FAILED)
            putExtra(StatusSplashActivity.EXTRA_TITLE, title)
            putExtra(StatusSplashActivity.EXTRA_MESSAGE, message)
            putExtra(StatusSplashActivity.EXTRA_CATEGORY, category)
        }
        activity.startActivity(intent)
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

