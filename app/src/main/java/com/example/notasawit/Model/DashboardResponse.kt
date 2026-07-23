package com.example.notasawit.Model

data class DashboardResponse(
    val success: Boolean,
    val data: DashboardData?
)

data class DashboardData(
    val jumlah_petani: Int,
    val jumlah_lahan: Double,
    val pemasukan: List<Int>,
    val pengeluaran: List<PengeluaranData>
)

data class PengeluaranData(
    val jenis: String,
    val total: Int
)
