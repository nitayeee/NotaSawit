package com.example.notasawit.Room.DetailKegiatan



import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "detail_kegiatan")
data class DetailKegiatanEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val kegiatanId: Int,

    val lahanId: Int

)