package com.example.notasawit.Model

import com.example.notasawit.Room.JenisKegiatan.JenisKegiatanEntity

data class Kegiatan(

    val kegiatan_id: Int,

    val petani_id: Int,

    val jenis_kegiatan_id: Int,

    val kegiatan_tanggal: String,

    val kegiatan_jumlah: Int,

    val kegiatan_satuan: String,

    val kegiatan_ket: String
)