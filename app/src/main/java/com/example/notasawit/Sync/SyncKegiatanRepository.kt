package com.example.notasawit.Sync

import android.content.Context
import android.util.Log
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncKegiatanRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    // Ubah fungsi menjadi mengembalikan Boolean (true jika SEMUA sukses, false jika ada yang gagal)
    suspend fun sync(): Boolean = withContext(Dispatchers.IO) {
        val kegiatanList = database.KegiatanDao().getUnsynced()

        if (kegiatanList.isEmpty()) return@withContext true

        var allSuccess = true

        kegiatanList.forEach { kegiatan ->
            val detailLahan = database.DetailKegiatanDao().gethByKegiatan(kegiatan.localId)
            val lahanIds = detailLahan.map { it.lahanId }

            Log.d("SYNC", "==========================")
            Log.d("SYNC", "Mengirim Kegiatan ID: ${kegiatan.localId}")

            try {
                // KUNCI UTAMA: Gunakan .execute() agar codingan menunggu response server (Synchronous)
                // Jika postKegiatan belum mendukung .execute(), pastikan method tersebut mengembalikan objek 'Call'
                val call = PetaniApi.postKegiatan(
                    kegiatanTanggal = kegiatan.kegiatan_tanggal,
                    kegiatanJumlah = kegiatan.kegiatan_jumlah,
                    kegiatanSatuan = kegiatan.kegiatan_satuan,
                    jenisKegiatanId = kegiatan.kegiatan_jenis,
                    petaniId = kegiatan.petani_id,
                    kegiatanKet = kegiatan.kegiatan_ket,
                    lahanIds = lahanIds
                )

                val response = call.execute() // <--- Menunggu response dari server di thread IO

                val body = response.body?.string() ?: ""
                Log.d("API", "CODE = ${response.code}")

                if (response.isSuccessful) {
                    Log.d("API", "isSuccessful = true. Response = $body")

                    // Hapus data lokal LANGSUNG di thread yang sama, tidak perlu CoroutineScope baru
                    database.DetailKegiatanDao().deleteByKegiatan(kegiatan.localId)
                    database.KegiatanDao().deleteById(kegiatan.localId)

                    Log.d("SYNC", "Kegiatan ${kegiatan.localId} berhasil dihapus dari Room")
                } else {
                    Log.e("SYNC", "Sync gagal untuk ID ${kegiatan.localId}: ${response.code}")
                    allSuccess = false // Set falg menjadi false agar worker tahu ada yang gagal
                }

            } catch (e: Exception) {
                Log.e("SYNC", "Gagal sync karena koneksi/RTO untuk ID ${kegiatan.localId}", e)
                allSuccess = false
            }
        }

        return@withContext allSuccess
    }
}