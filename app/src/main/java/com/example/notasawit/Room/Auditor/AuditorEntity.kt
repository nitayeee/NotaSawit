package com.example.notasawit.Room.Auditor

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_auditor")
data class AuditorEntity(
    @PrimaryKey val idAuditor: Int,
    val namaAuditor: String,
    val username: String // Opsional, jika nanti butuh login/pencocokan data
)