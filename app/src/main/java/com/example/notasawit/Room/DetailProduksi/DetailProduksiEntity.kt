package com.example.notasawit.Room.DetailProduksi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detail_produksi")
data class DetailProduksiEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val produksiId: Int,

    val lahanId: Int,

    val jumlahProduksi: Double = 0.0,

    val subtotalPendapatan: Double = 0.0

)