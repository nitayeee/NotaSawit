package com.example.notasawit.Sync

import android.content.Context
import android.util.Log
import com.example.notasawit.Network.PetaniApi
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
                // Tandai sementara agar tidak diambil ulang jika ada proses bersamaan
                database.KegiatanDao().updateSynced(kegiatan.localId)

                val detailLahan = database.DetailKegiatanDao().gethByKegiatan(kegiatan.localId)
                val lahanIds = detailLahan.map { it.lahanId }

                Log.d("SYNC", "==========================")
                Log.d("SYNC", "Mengirim Kegiatan ID: ${kegiatan.localId}")

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
                    Log.d("API", "CODE = ${response.code}")

                    if (response.isSuccessful) {
                        Log.d("API", "isSuccessful = true. Response = $body")
                        database.DetailKegiatanDao().deleteByKegiatan(kegiatan.localId)
                        database.KegiatanDao().deleteById(kegiatan.localId)
                        Log.d("SYNC", "Kegiatan ${kegiatan.localId} berhasil dikirim ke server dan dihapus dari Room")
                    } else {
                        Log.e("SYNC", "Sync gagal untuk ID ${kegiatan.localId}: ${response.code}")
                        database.KegiatanDao().updateUnsynced(kegiatan.localId)
                        allSuccess = false
                    }

                } catch (e: Exception) {
                    Log.e("SYNC", "Gagal sync karena koneksi/RTO untuk ID ${kegiatan.localId}", e)
                    database.KegiatanDao().updateUnsynced(kegiatan.localId)
                    allSuccess = false
                }
            }

            return@withContext allSuccess
        } finally {
            syncMutex.unlock()
        }
    }
}