package com.example.notasawit.Model

data class PetaniSummaryResponse(
    val success: Boolean,
    val data: PetaniSummaryData?
)

data class PetaniSummaryData(
    val pemasukan_bulan_ini: Double,
    val pemasukan_bulan_lalu: Double,
    val pengeluaran_bulan_ini: Double,
    val pengeluaran_bulan_lalu: Double
)
