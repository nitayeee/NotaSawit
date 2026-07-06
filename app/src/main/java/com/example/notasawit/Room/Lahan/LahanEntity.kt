package com.example.notasawit.Room.Lahan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lahan")
data class LahanEntity(

    @PrimaryKey
    val lahan_id: Int,

    val petani_id: Int,

    val lahan_nama: String
)