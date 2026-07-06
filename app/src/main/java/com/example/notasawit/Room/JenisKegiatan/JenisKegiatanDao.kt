package com.example.notasawit.Room.JenisKegiatan

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface JenisKegiatanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJenisKegiatan(data: List<JenisKegiatanEntity>)

    @Query("SELECT * FROM jenis_kegiatan")
    suspend fun getAllJenisKegiatan(): List<JenisKegiatanEntity>

}