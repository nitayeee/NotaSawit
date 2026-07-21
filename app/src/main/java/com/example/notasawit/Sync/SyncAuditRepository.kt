package com.example.notasawit.Sync

import android.content.Context
import android.util.Log
import com.example.notasawit.Admin.AuditInternal.AuditDao.AuditDao
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncAuditRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun syncAudit(): Boolean = withContext(Dispatchers.IO) {
        val auditList = database.auditDao().getUnsyncedAudit()

        if (auditList.isEmpty()) return@withContext true

        var allSuccess = true
        
        val sharedPref = context.getSharedPreferences("NOTASAWIT_PREF", Context.MODE_PRIVATE)
        val userId = sharedPref.getInt("user_id", 0)

        auditList.forEach { audit ->
            Log.d("SYNC_AUDIT", "==========================")
            Log.d("SYNC_AUDIT", "Mengirim Audit ID Lokal: ${audit.idAudit}")

            try {
                val call = PetaniApi.postAuditInternal(
                    idAudit = audit.idAudit,
                    userId = userId,
                    tanggal = audit.tanggal,
                    desa = audit.desa,
                    namaAuditor = audit.namaAuditor,
                    namaPetani = audit.namaPetani,
                    pdfPath = audit.pdfPath
                )

                val response = call.execute()
                val body = response.body?.string() ?: ""

                Log.d("API_AUDIT", "CODE = ${response.code}")

                if (response.isSuccessful) {
                    if (body.contains("<!DOCTYPE html>") || body.contains("<html")) {
                        Log.e("API_AUDIT", "Kritis! Server merespon 200 tapi isinya HTML Login. Data lokal TIDAK disync.")
                        allSuccess = false
                    } else {
                        Log.d("API_AUDIT", "Sukses masuk server asli: $body")
                        database.auditDao().markAsSynced(audit.idAudit)
                        Log.d("SYNC_AUDIT", "Data Utama Audit ID ${audit.idAudit} berhasil disync.")
                    }
                } else {
                    Log.e("SYNC_AUDIT", "Gagal ke server untuk Audit ${audit.idAudit}: ${response.code}, Body: $body")
                    allSuccess = false
                }

            } catch (e: Exception) {
                Log.e("SYNC_AUDIT", "Crash Jaringan/RTO saat kirim Audit ID ${audit.idAudit}", e)
                allSuccess = false
            }
        }

        return@withContext allSuccess
    }
}
