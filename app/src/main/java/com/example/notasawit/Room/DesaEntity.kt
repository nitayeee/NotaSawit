package com.example.notasawit.Room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "desa")
data class DesaEntity(

    @PrimaryKey
    val idDesa: Int,

    val namaDesa: String

)