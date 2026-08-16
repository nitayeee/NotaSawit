package com.example.notasawit.Sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncPengeluaranRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    // Mengembalikan Boolean agar Worker tahu status akhirnya
    suspend fun syncPengeluaran(): Boolean = withContext(Dispatchers.IO) {
        // 1. Ambil data pengeluaran lokal yang belum tersinkron (isSynced = false)
        val pengeluaranList = database.PengeluaranDao().getUnsynced() // Pastikan ada query getUnsynced() di Dao Anda

        if (pengeluaranList.isEmpty()) return@withContext true

        var allSuccess = true

        pengeluaranList.forEach { biaya_operasional ->
            // 2. Ambil data lahan yang terhubung dengan pengeluaran ini
            val detailLahan = database.DetailPengeluaranDao().getByPengeluaran(biaya_operasional.localId) // Sesuaikan method Dao Anda

            val lahanIds = detailLahan.map { it.lahanId }

            if (lahanIds.isEmpty()) {
                // Jika tidak ada lahan terkait, lewati atau hapus data kotor ini
                database.PengeluaranDao().deleteById(biaya_operasional.localId)
                return@forEach
            }

            Log.d("SYNC_PENGELUARAN", "==========================")
            Log.d("SYNC_PENGELUARAN", "Mengirim Pengeluaran ID Lokal: ${biaya_operasional.localId}")

            try {
                val imageUri = biaya_operasional.imagePath?.let { Uri.parse(it) }

                // 3. Panggil postPengeluaran bentukan OkHttp yang baru (.Call)
                val call = PetaniApi.postPengeluaran(
                    context = context,
                    biayaTanggal = biaya_operasional.biaya_tanggal, // Sudah yyyy-MM-dd
                    biayaJumlah = biaya_operasional.biaya_jumlah,
                    biayaJenis = biaya_operasional.biaya_jenis,
                    biayaTotal = biaya_operasional.biaya_total,
                    biayaNama = biaya_operasional.biaya_nama,
                    petaniId = biaya_operasional.petani_id,
                    lahanIds = lahanIds, // Kirim list lahan sekaligus
                    biayaKet = biaya_operasional.biaya_ket ?: "",
                    imageUri = imageUri
                )

                // 4. Eksekusi secara synchronous (Menunggu respons Laravel)
                val response = call.execute()
                val body = response.body?.string() ?: ""

                Log.d("API_PENGELUARAN", "CODE = ${response.code}")

                if (response.isSuccessful) {
                    if (body.contains("<!DOCTYPE html>") || body.contains("<html")) {
                        Log.e("API_PENGELUARAN", "Kritis! Server merespon 200 tapi isinya HTML Login. Data lokal TIDAK dihapus.")
                        allSuccess = false
                    } else {
                        Log.d("API_PENGELUARAN", "Sukses masuk server asli: $body")
                        // Hapus detail dan pengeluaran utama
                        database.DetailPengeluaranDao().deleteByPengeluaran(biaya_operasional.localId)
                        database.PengeluaranDao().deleteById(biaya_operasional.localId)
                        Log.d("SYNC_PENGELUARAN", "Data Utama Pengeluaran ID ${biaya_operasional.localId} bersih dari lokal Room.")
                    }
                } else {
                    Log.e("SYNC_PENGELUARAN", "Gagal ke server untuk Pengeluaran ${biaya_operasional.localId}: Code ${response.code} | Response: $body")
                    allSuccess = false
                }

            } catch (e: Exception) {
                Log.e("SYNC_PENGELUARAN", "Crash Jaringan/RTO saat kirim Pengeluaran ID ${biaya_operasional.localId}", e)
                allSuccess = false
            }
        }

        return@withContext allSuccess
    }
}