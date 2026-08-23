package com.example.notasawit.Admin.KunjunganLahan.model

enum class KunjunganQuestionType {
    RADIO_ADA_TIDAK,
    ESSAY
}

sealed class KunjunganItem {

    data class Header(
        val title: String
    ) : KunjunganItem()

    data class Question(
        val key: String,
        val question: String,
        val standard: String,
        val type: KunjunganQuestionType = KunjunganQuestionType.RADIO_ADA_TIDAK,
        var answer: Boolean? = null,
        var textAnswer: String? = null
    ) : KunjunganItem()

}
