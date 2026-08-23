package com.example.notasawit.RiwayatKeuangan

import com.google.gson.annotations.SerializedName
data class RiwayatItem(
    @SerializedName("id") val id: Int,
    @SerializedName("judul") val judul: String,
    @SerializedName("tanggal") val tanggal: String,
    @SerializedName("nominal") val nominal: Double,
    @SerializedName("tipe") val tipe: String,

    // Wajib disamakan dengan key di JSON API Laravel ("lahan_nama")
    @SerializedName("lahan_nama") val lahanNama: String?,
    @SerializedName("lahan_id") val lahanId: Int? = null,
    @SerializedName("jumlah_tbs") val jumlahTbs: Double? = null,

    // Wajib disamakan dengan key di JSON API Laravel ("source_table")
    @SerializedName("source_table") val sourceTable: String,

    @SerializedName("is_read") val isRead: Int?
)