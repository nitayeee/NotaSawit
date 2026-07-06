package com.example.notasawit.Room.Produksi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produksi")
data class ProduksiEntity(

    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,

    val tanggal: String,

    val jumlahTbs: Int,

    val hargaTbs: Double,

    val petaniId: Int,

    val lahanId: Int,

    val catatan: String,

    val imagePath: String?
)