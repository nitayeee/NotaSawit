package com.example.notasawit.Admin.Beranda

data class Pengingat(
    val id: Int,
    val judul: String,
    val pesan: String,
    val deadline: String,
    val isDone: Boolean
)
