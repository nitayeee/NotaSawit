package com.example.notasawit.Sync

import android.content.Context
import android.util.Log
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncKunjunganRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun syncKunjungan(): Boolean = withContext(Dispatchers.IO) {
        val kunjunganList = database.KunjunganLahanDao().getUnsyncedKunjunganLahan()

        if (kunjunganList.isEmpty()) return@withContext true

        var allSuccess = true

        kunjunganList.forEach { kunjungan ->
            Log.d("SYNC_KUNJUNGAN", "==========================")
            Log.d("SYNC_KUNJUNGAN", "Mengirim Kunjungan ID Lokal: ${kunjungan.idKunjungan}")

            try {
                val call = PetaniApi.postKunjunganLapangan(
                    tanggalKunjungan = kunjungan.tanggal,
                    desaKebun = kunjungan.desaKebun,
                    desaKepengurusan = kunjungan.desaKepengurusan,
                    namaAuditor = kunjungan.namaAuditor,
                    namaPetani = kunjungan.namaPetani,
                    statusKunjungan = kunjungan.statusKunjungan,
                    keterangan = kunjungan.ringkasanTemuan,
                    periode = kunjungan.periode,
                    visitAttempt = kunjungan.visitAttempt,
                    pdfPath = kunjungan.pdfPath
                )

                val response = call.execute()
                val body = response.body?.string() ?: ""

                Log.d("API_KUNJUNGAN", "CODE = ${response.code}")

                if (response.isSuccessful) {
                    if (body.contains("<!DOCTYPE html>") || body.contains("<html")) {
                        Log.e("API_KUNJUNGAN", "Kritis! Server merespon 200 tapi isinya HTML Login. Data lokal TIDAK disync.")
                        allSuccess = false
                    } else {
                        Log.d("API_KUNJUNGAN", "Sukses masuk server asli: $body")
                        database.KunjunganLahanDao().markAsSynced(kunjungan.idKunjungan)
                        Log.d("SYNC_KUNJUNGAN", "Data Utama Kunjungan ID ${kunjungan.idKunjungan} berhasil disync.")
                    }
                } else {
                    Log.e("SYNC_KUNJUNGAN", "Gagal ke server untuk Kunjungan ${kunjungan.idKunjungan}: ${response.code}, Body: $body")
                    allSuccess = false
                }

            } catch (e: Exception) {
                Log.e("SYNC_KUNJUNGAN", "Crash Jaringan/RTO saat kirim Kunjungan ID ${kunjungan.idKunjungan}", e)
                allSuccess = false
            }
        }

        return@withContext allSuccess
    }
}
