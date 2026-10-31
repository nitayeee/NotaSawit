package com.example.notasawit.Sync

import android.content.Context
import android.util.Log
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Notification.NotificationHelper
import com.example.notasawit.Room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext

class SyncKegiatanRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    companion object {
        private val syncMutex = Mutex()
    }

    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        if (!syncMutex.tryLock()) {
            Log.d("SYNC_KEGIATAN", "Sinkronisasi kegiatan sedang berjalan di thread lain, skip.")
            return@withContext true
        }

        try {
            val kegiatanList = database.KegiatanDao().getUnsynced()

            if (kegiatanList.isEmpty()) return@withContext true

            var allSuccess = true

            kegiatanList.forEach { kegiatan ->
                val detailLahan = database.DetailKegiatanDao().gethByKegiatan(kegiatan.localId)
                val lahanIds = detailLahan.map { it.lahanId }

                if (lahanIds.isEmpty()) {
                    Log.w("SYNC_KEGIATAN", "Kegiatan lokal ${kegiatan.localId} tidak memiliki data lahan, hapus dari Room")
                    database.KegiatanDao().deleteById(kegiatan.localId)
                    return@forEach
                }

                Log.d("SYNC_KEGIATAN", "==========================")
                Log.d("SYNC_KEGIATAN", "Mengirim Kegiatan ID: ${kegiatan.localId}")

                try {
                    val call = PetaniApi.postKegiatan(
                        kegiatanTanggal = kegiatan.kegiatan_tanggal,
                        kegiatanJumlah = kegiatan.kegiatan_jumlah,
                        kegiatanSatuan = kegiatan.kegiatan_satuan,
                        jenisKegiatanId = kegiatan.kegiatan_jenis,
                        petaniId = kegiatan.petani_id,
                        kegiatanKet = kegiatan.kegiatan_ket,
                        namaBahan = kegiatan.nama_bahan,
                        jenisLimbah = kegiatan.jenis_limbah,
                        statusLimbah = kegiatan.status_limbah,
                        namaKegiatan = kegiatan.nama_kegiatan,
                        lahanIds = lahanIds
                    )

                    val response = call.execute()
                    val body = response.body?.string() ?: ""
                    Log.d("API_KEGIATAN", "CODE = ${response.code}")

                    if (response.isSuccessful) {
                        if (body.contains("<!DOCTYPE html>") || body.contains("<html")) {
                            Log.e("API_KEGIATAN", "Kritis! Server merespon 200 tapi isinya HTML Login. Data lokal TIDAK dihapus.")
                            allSuccess = false
                        } else {
                            Log.d("API_KEGIATAN", "Sukses masuk server asli: $body")

                            val serverId = try {
                                val jsonObj = org.json.JSONObject(body)
                                val dataObj = jsonObj.optJSONObject("data")
                                dataObj?.optInt("kegiatan_id", 0)
                                    ?: dataObj?.optInt("id", 0)
                                    ?: jsonObj.optInt("kegiatan_id", jsonObj.optInt("id", 0))
                            } catch (e: Exception) {
                                0
                            }

                            // Tampilkan notifikasi sistem di HP
                            NotificationHelper.showKegiatanNotification(
                                context = context,
                                kegiatanId = serverId,
                                namaKegiatan = kegiatan.nama_kegiatan,
                                tanggal = kegiatan.kegiatan_tanggal
                            )

                            database.DetailKegiatanDao().deleteByKegiatan(kegiatan.localId)
                            database.KegiatanDao().deleteById(kegiatan.localId)
                            Log.d("SYNC_KEGIATAN", "Kegiatan ${kegiatan.localId} berhasil dikirim ke server dan dihapus dari Room")
                        }
                    } else {
                        Log.e("SYNC_KEGIATAN", "Sync gagal untuk ID ${kegiatan.localId}: ${response.code} | $body")
                        allSuccess = false
                    }

                } catch (e: Exception) {
                    Log.e("SYNC_KEGIATAN", "Gagal sync karena koneksi/RTO untuk ID ${kegiatan.localId}", e)
                    allSuccess = false
                }
            }

            return@withContext allSuccess
        } finally {
            syncMutex.unlock()
        }
    }
}