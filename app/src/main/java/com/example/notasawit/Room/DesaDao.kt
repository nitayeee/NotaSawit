package com.example.notasawit.Room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DesaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDesa(data: List<DesaEntity>)

    @Query("SELECT * FROM desa")
    suspend fun getAllDesa(): List<DesaEntity>

}