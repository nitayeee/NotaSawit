package com.example.notasawit.Admin.AuditInternal.model

sealed class AuditItem {

    data class Header(
        val title: String
    ) : AuditItem()

    data class Question(
        val key: String,
        val question: String,
        var answer: Boolean? = null
    ) : AuditItem()

}