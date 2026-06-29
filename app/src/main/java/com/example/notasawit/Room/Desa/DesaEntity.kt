package com.example.notasawit.Room.Desa

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "desa_lokal")
data class DesaEntity(
    @PrimaryKey val idDesa: Int,
    val namaDesa: String
)