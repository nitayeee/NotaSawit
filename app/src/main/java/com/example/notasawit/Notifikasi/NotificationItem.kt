package com.example.notasawit.Notifikasi

data class NotificationItem(
    val id_audit: Int,
    val tanggal: String,
    val nama_auditor: String,
    var is_read: Int,
    val path_file_kunjungan: String?
)
