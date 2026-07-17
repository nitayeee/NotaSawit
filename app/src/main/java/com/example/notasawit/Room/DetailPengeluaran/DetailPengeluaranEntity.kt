package com.example.notasawit.Room.DetailPengeluaran

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detail_pengeluaran")
data class DetailPengeluaranEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val biaya_operasional_id: Int,

    val lahanId: Int

)