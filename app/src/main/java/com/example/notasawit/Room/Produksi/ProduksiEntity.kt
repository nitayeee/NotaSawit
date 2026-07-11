package com.example.notasawit.Room.Produksi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produksi")
data class ProduksiEntity(

    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,

    val produksi_tanggal: String,

    val jumlah_tbs: Int,

    val harga_tbs: Double,

    val total_pendapatan: Double,

    val petaniId: Int,

    val produksi_ket: String,

    val imagePath: String?,
    val isSynced: Boolean = false
)