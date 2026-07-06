package com.example.notasawit.Room.DetailKegiatan


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DetailKegiatanDao {

    @Insert
    suspend fun insert(detail: DetailKegiatanEntity)

    @Query("SELECT * FROM detail_kegiatan")
    suspend fun getAll(): List<DetailKegiatanEntity>

    @Query("SELECT * FROM detail_kegiatan WHERE kegiatanId = :id")
    suspend fun getByKegiatan(id: Int): List<DetailKegiatanEntity>

}