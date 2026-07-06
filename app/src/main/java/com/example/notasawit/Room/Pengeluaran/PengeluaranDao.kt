package com.example.notasawit.Room.Pengeluaran

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PengeluaranDao {

    @Insert
    suspend fun insert(data: PengeluaranEntity)

    @Query("SELECT * FROM biaya_operasional")
    suspend fun getAll(): List<PengeluaranEntity>

    @Delete
    suspend fun delete(pengeluaran: PengeluaranEntity)
}