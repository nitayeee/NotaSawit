package com.example.notasawit.Room.JenisKegiatan

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "jenis_kegiatan")
data class JenisKegiatanEntity(

    @PrimaryKey
    val id_jenis: Int,
    val nama_jenis: String,
    val ikon: String
)