package com.example.notasawit.Repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import com.example.notasawit.Room.Produksi.ProduksiEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class SyncProduksiRepository(
    private val context: Context,
    private val database: AppDatabase
) {

    suspend fun syncProduksi() {

        val listProduksi = database.ProduksiDao().getAll()

        for (produksi in listProduksi) {

            val berhasil = uploadProduksi(produksi)

            if (berhasil) {
                database.ProduksiDao().delete(produksi)

                val sisa = database.ProduksiDao().getAll()

                Log.d("SYNC", "Sisa data = ${sisa.size}")
            }

        }
    }

    private suspend fun uploadProduksi(
        produksi: ProduksiEntity
    ): Boolean = withContext(Dispatchers.IO) {

        suspendCoroutine { continuation ->

            PetaniApi.postProduksi(

                context = context,

                produksiTanggal = produksi.tanggal,

                jumlahTbs = produksi.jumlahTbs,

                hargaTbs = produksi.hargaTbs,

                petaniId = produksi.petaniId,

                lahanId = produksi.lahanId,

                produksiKet = produksi.catatan,

                imageUri = produksi.imagePath?.let {
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