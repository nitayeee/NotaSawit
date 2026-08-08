package com.example.notasawit.Room.Petani

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "master_petani")
data class PetaniEntity(
    @PrimaryKey val idPetani: Int,
    val namaPetani: String,
    val namaDesa: String,
    val desaId: Int
)