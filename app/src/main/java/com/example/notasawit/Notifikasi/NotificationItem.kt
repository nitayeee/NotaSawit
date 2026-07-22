package com.example.notasawit.Notifikasi

data class NotificationItem(
    val id: Int,
    val type: String,
    val title: String,
    val message: String,
    val tanggal: String,
    var is_read: Int,
    val data_url: String?
)
