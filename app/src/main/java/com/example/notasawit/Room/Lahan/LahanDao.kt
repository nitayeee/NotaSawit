package com.example.notasawit.Room.Lahan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LahanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLahan(data: List<LahanEntity>)

    @Query("SELECT * FROM lahan")
    suspend fun getAllLahan(): List<LahanEntity>

}