package com.example.notasawit.Admin.KunjunganLahan.model

sealed class KunjunganItem {

    data class Header(
        val title: String
    ) : KunjunganItem()

    data class Question(
        val key: String,
        val question: String,
        val standard: String,
        var answer: Boolean? = null
    ) : KunjunganItem()

}
