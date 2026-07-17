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

        return try {
            // 1. Jalankan Sinkronisasi Kegiatan
            val isKegiatanSuccess = SyncKegiatanRepository(applicationContext, db).sync()

            // 2. Jalankan Sinkronisasi Pemasukan / Produksi
            val isProduksiSuccess = SyncProduksiRepository(applicationContext, db).syncProduksi()

            val isPengeluaranSuccess = SyncPengeluaranRepository(applicationContext, db).syncPengeluaran()

            // 3. Evaluasi hasil akhir kedua sinkronisasi
            if (isKegiatanSuccess && isProduksiSuccess && isPengeluaranSuccess) {
                Log.d("WORKER", "Semua data (Kegiatan & Pemasukan & Pengeluaran) sukses tersinkron!")
                Result.success() // Tugas selesai dengan sempurna
            } else {
                Log.d("WORKER", "Ada data gagal kirim (kegiatan=$isKegiatanSuccess, produksi=$isProduksiSuccess, pengeluaran=$isPengeluaranSuccess). Menjadwalkan ulang...")
                Result.retry() // Coba lagi nanti saat internet stabil
            }
        } catch (e: Exception) {
            Log.e("WORKER", "Terjadi error fatal saat sync: ${e.message}")
            Result.retry() // Coba lagi jika terjadi crash koneksi atau masalah runtime
        }
    }
}