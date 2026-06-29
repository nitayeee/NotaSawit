package com.example.notasawit.Network

import kotlinx.serialization.Serializable

@Serializable
data class petani(
    val petani_nama: String,
    val petani_username: String,
    val petani_tanggal_lahir: String,
    val petani_jenis_kelamin: String,
    val petani_email: String,
    val petani_no_hp: String,
    val petani_alamat: String,
    val petani_desa: String,
    val petani_status: String,
    val petani_pin: String
)