package com.example.notasawit.Sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.notasawit.Room.AppDatabase

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d("WORKER", "SyncWorker dijalankan")
        val db = AppDatabase.getDatabase(applicationContext)

        SyncKegiatanRepository(
            applicationContext,
            db
        ).sync()

        return Result.success()
    }
}