package com.example.notasawit.Room.AuditEntity

import androidx.room.Entity

@Entity(
    tableName = "audit_answer",
    primaryKeys = ["idAudit", "questionKey"]
)
data class AuditAnswer(
    val idAudit: String,
    val questionKey: String,
    val answer: Boolean?
)
