package com.example.notasawit.Room.Pengeluaran

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "biaya_operasional")
data class PengeluaranEntity(

    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,

    val biaya_tanggal: String,

    val biaya_jumlah: Int,

    val biaya_nama: String,

    val biaya_jenis: String,
    val petani_id: Int,
    val biaya_total: Double,

    val biaya_ket: String,

    val imagePath: String?,
    val isSynced: Boolean = false

)