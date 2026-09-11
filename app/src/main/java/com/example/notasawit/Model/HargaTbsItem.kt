package com.example.notasawit.Model

data class HargaTbsItem(
    val harga_tbs_id: Int,
    val harga_dinas: Double,
    val harga_pt_sar: Double,
    val tanggal_berlaku: String,
    val created_at: String?
)
