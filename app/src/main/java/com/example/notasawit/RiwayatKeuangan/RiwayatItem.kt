package com.example.notasawit.RiwayatKeuangan

data class RiwayatItem(
    val id: Int,
    val judul: String,
    val tanggal: String,
    val nominal: Double,
    val tipe: String,
    val lahanNama: String?,
    val sourceTable: String
)