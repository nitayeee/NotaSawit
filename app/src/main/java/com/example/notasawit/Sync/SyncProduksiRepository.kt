package com.example.notasawit.Sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncProduksiRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    // Mengembalikan Boolean agar Worker tahu status akhirnya
    suspend fun syncProduksi(): Boolean = withContext(Dispatchers.IO) {
        // 1. Ambil data produksi lokal yang belum tersinkron (isSynced = false)
        val produksiList = database.ProduksiDao().getUnsynced() // Pastikan ada query getUnsynced() di Dao Anda

        if (produksiList.isEmpty()) return@withContext true

        var allSuccess = true

        produksiList.forEach { produksi ->
            // 2. Ambil data lahan yang terhubung dengan produksi ini
            val detailLahan = database.DetailProduksiDao().getByProduksi(produksi.localId) // Sesuaikan method Dao Anda

            if (detailLahan.isEmpty()) {
                database.ProduksiDao().deleteById(produksi.localId)
                return@forEach
            }

            Log.d("SYNC_PRODUKSI", "Mengirim Produksi ID Lokal: ${produksi.localId}")

            try {
                val imageUri = produksi.imagePath?.let { android.net.Uri.parse(it) }

                val call = com.example.notasawit.Network.PetaniApi.postProduksi(
                    context = context,
                    produksiTanggal = produksi.produksi_tanggal,
                    jumlahTbs = produksi.jumlah_tbs,
                    hargaTbs = produksi.harga_tbs,
                    petaniId = produksi.petaniId,
                    detailLahan = detailLahan,
                    produksiKet = produksi.produksi_ket ?: "",
                    totalPendapatan = produksi.total_pendapatan,
                    imageUri = imageUri
                )

                // 4. Eksekusi secara synchronous (Menunggu respons Laravel)
                val response = call.execute()
                val body = response.body?.string() ?: ""

                Log.d("API_PRODUKSI", "CODE = ${response.code}")

                if (response.isSuccessful) {
                    if (body.contains("<!DOCTYPE html>") || body.contains("<html")) {
                        Log.e("API_PRODUKSI", "Kritis! Server merespon 200 tapi isinya HTML Login. Data lokal TIDAK dihapus.")
                        allSuccess = false
                    } else {
                        Log.d("API_PRODUKSI", "Sukses masuk server asli: $body")
                        // Hapus detail dan produksi utama
                        database.DetailProduksiDao().deleteByProduksi(produksi.localId)
                        database.ProduksiDao().deleteById(produksi.localId)
                        Log.d("SYNC_PRODUKSI", "Data Utama Produksi ID ${produksi.localId} bersih dari lokal Room.")
                    }
                } else {
                    Log.e("SYNC_PRODUKSI", "Gagal ke server untuk Produksi ${produksi.localId}: ${response.code}")
                    allSuccess = false
                }

            } catch (e: Exception) {
                Log.e("SYNC_PRODUKSI", "Crash Jaringan/RTO saat kirim Produksi ID ${produksi.localId}", e)
                allSuccess = false
            }
        }

        return@withContext allSuccess
    }
}