package com.example.notasawit.Model

import com.google.gson.annotations.SerializedName

// ==========================================
// 1. MODEL PRODUKSI (UTAMA)
// ==========================================
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
    val is_read: Int?,
    val petani: PetaniDetail?,

    // UBAH: Tidak lagi berupa satu LahanDetail langsung,
    // melainkan List yang berisi rincian beserta lahannya masing-masing
    val detail_produksi: List<ItemDetailProduksi>?
)

// Model baru untuk menampung item detail_produksi dari API
data class ItemDetailProduksi(
    val id: Int,
    @SerializedName("jumlah_tbs", alternate = ["jumlah_tbs_detail", "jumlah_produksi"])
    val jumlah_tbs_detail: Double?, // Menyesuaikan field dari Laravel
    val harga_tbs_detail: Double?,
    @SerializedName("subtotal_pendapatan", alternate = ["subtotal"])
    val subtotal_pendapatan: Double?,
    val lahan: LahanDetail?      // Sekarang Lahan menempel di sini
)


// ==========================================
// 2. MODEL BIAYA OPERASIONAL / PENGELUARAN (UTAMA)
// ==========================================
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
    val is_read: Int?,
    val petani: PetaniDetail?,

    // UBAH: Sama seperti produksi, diganti menjadi List
    val detail_biaya: List<ItemDetailBiaya>?
)

// Model baru untuk menampung item detail_biaya dari API
data class ItemDetailBiaya(
    val id: Int,
    val nama_detail: String?, // Menyesuaikan field dari Laravel
    val subtotal: Double?,
    val lahan: LahanDetail?   // Lahan ditarik ke sini
)


// ==========================================
// 3. MODEL PELENGKAP (TETAP SAMA)
// ==========================================
data class PetaniDetail(
    val id: Int?, // Gunakan nullable untuk berjaga-jaga jika ID null
    val nama: String?
)

data class LahanDetail(
    val id: Int?,
    val nama: String?
)