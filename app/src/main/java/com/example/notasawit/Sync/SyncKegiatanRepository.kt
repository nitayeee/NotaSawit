package com.example.notasawit.Sync

import android.content.Context
import android.util.Log
import com.example.notasawit.Network.PetaniApi
import com.example.notasawit.Room.AppDatabase
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException

class SyncKegiatanRepository(

    private val context: Context,
    private val database: AppDatabase

) {

    suspend fun sync() {

        val kegiatanList =
            database.KegiatanDao().getUnsynced()

        kegiatanList.forEach { kegiatan ->

            // Ambil semua lahan dari kegiatan ini
            val detailLahan =
                database.DetailKegiatanDao()
                    .getByKegiatan(kegiatan.localId)

            val lahanIds =
                detailLahan.map { it.lahanId }
            Log.d("SYNC", "==========================")
            Log.d("SYNC", "Tanggal : ${kegiatan.kegiatan_tanggal}")
            Log.d("SYNC", "Jumlah  : ${kegiatan.kegiatan_jumlah}")
            Log.d("SYNC", "Satuan  : ${kegiatan.kegiatan_satuan}")
            Log.d("SYNC", "Jenis   : ${kegiatan.kegiatan_jenis}")
            Log.d("SYNC", "Petani  : ${kegiatan.petani_id}")
            Log.d("SYNC", "Ket     : ${kegiatan.kegiatan_ket}")
            Log.d("SYNC", "Lahan   : $lahanIds")

            PetaniApi.postKegiatan(

                kegiatanTanggal = kegiatan.kegiatan_tanggal,

                kegiatanJumlah = kegiatan.kegiatan_jumlah,

                kegiatanSatuan = kegiatan.kegiatan_satuan,

                jenisKegiatanId = kegiatan.kegiatan_jenis,

                petaniId = kegiatan.petani_id,

                kegiatanKet = kegiatan.kegiatan_ket,

                lahanIds = lahanIds,

                callback = object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {

                        Log.e(
                            "SYNC",
                            "Gagal sync kegiatan",
                            e
                        )

                    }

                    override fun onResponse(call: Call, response: Response) {

                        val body = response.body?.string() ?: ""

                        Log.d("API", "CODE = ${response.code}")

                        val index = body.indexOf("Exception")

                        if (index != -1) {
                            Log.d("API", body.substring(index, minOf(index + 1500, body.length)))
                        } else {
                            Log.d("API", body.take(1500))
                        }
                    }

                }

            )

        }

    }

}