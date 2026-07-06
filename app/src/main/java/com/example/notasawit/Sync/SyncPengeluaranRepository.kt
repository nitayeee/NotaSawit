package com.example.notasawit.Sync

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.Pengeluaran.PengeluaranEntity
import com.example.notasawit.Room.Produksi.ProduksiEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SyncPengeluaranRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun syncPengeluaran() {

        val listPengeluaran = database.PengeluaranDao().getAll()

        for (pengeluaran in listPengeluaran) {

            val berhasil = uploadPengeluaran(pengeluaran)

            if (berhasil) {
                database.PengeluaranDao().delete(pengeluaran)

                val sisa = database.PengeluaranDao().getAll()

                Log.d("SYNC", "Sisa data = ${sisa.size}")
            }

        }
    }

    private suspend fun uploadPengeluaran(
        pengeluaran: PengeluaranEntity
    ): Boolean = withContext(Dispatchers.IO) {

        suspendCoroutine { continuation ->

            PetaniApi.postPengeluaran(

                context = context,

                biayaTanggal = pengeluaran.biaya_tanggal,

                biayaJumlah = pengeluaran.biaya_jumlah,

                biayaNama = pengeluaran.biaya_nama,

                biayaJenis = pengeluaran.biaya_jenis,

                biayaTotal = pengeluaran.biaya_total,

                biayaKet = pengeluaran.biaya_ket,
                lahanId = pengeluaran.lahan_id,
                petaniId = pengeluaran.petani_id,

                imageUri = pengeluaran.imagePath?.let {
                    Uri.parse(it)
                },

                callback = object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        continuation.resume(false)

                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {

                        continuation.resume(response.isSuccessful)

                    }

                }

            )


        }

    }

}