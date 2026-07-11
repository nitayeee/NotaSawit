package com.example.notasawit

import android.app.Application
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.notasawit.Sync.SyncWorker

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        Log.d("WORKER", "MyApplication jalan")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            "sync_data",
            ExistingWorkPolicy.REPLACE,
            request
        )

        Log.d("WORKER", "Worker didaftarkan")
    }
}