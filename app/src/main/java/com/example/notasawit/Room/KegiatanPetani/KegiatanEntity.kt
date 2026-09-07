package com.example.notasawit.Room.KegiatanPetani

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kegiatan")
data class KegiatanEntity(

    @PrimaryKey(autoGenerate = true)
    val localId: Int = 0,

    val kegiatan_tanggal: String,

    val kegiatan_jumlah: Int,

    val kegiatan_satuan: String,

    val kegiatan_jenis: Int = 0,

    val nama_kegiatan: String = "",

    val petani_id: Int,

    val kegiatan_ket: String = "",

    val nama_bahan: String = "",

    val jenis_limbah: String = "",

    val status_limbah: String = "Belum Disetor",

    val isSynced: Boolean = false
)