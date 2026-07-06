package com.example.notasawit.Model


data class ProduksiDetail(
    val id: Int,
    val produksi_tanggal: String,
    val jumlah_tbs: Int,
    val harga_tbs: Double,
    val total_pendapatan: Double,
    val status_validasi: String,
    val produksi_ket: String?,
    val produksi_bukti: String?,
    val produksi_bukti_url: String?,

    val petani: PetaniDetail?,
    val lahan: LahanDetail?
)

data class PengeluaranDetail(
    val id: Int,
    val biaya_tanggal: String,
    val biaya_jumlah: Int,
    val biaya_total: Double,
    val biaya_jenis: String,
    val biaya_nama: String,
    val biaya_ket: String?,
    val biaya_bukti: String?,
    val biaya_bukti_url: String?,

    val petani: PetaniDetail?,
    val lahan: LahanDetail?
)


data class PetaniDetail(
    val id: Int,
    val nama: String
)


data class LahanDetail(
    val id: Int,
    val nama: String
)