package com.example.notasawit.Room.Produksi

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ProduksiDao {

    @Insert
    suspend fun insert(data: ProduksiEntity)

    @Query("SELECT * FROM produksi")
    suspend fun getAll(): List<ProduksiEntity>

   @Delete
    suspend fun delete(produksi: ProduksiEntity)
}