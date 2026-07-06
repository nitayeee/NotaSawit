package com.example.notasawit.Room.KegiatanPetani

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.notasawit.Room.Pengeluaran.PengeluaranEntity

@Dao
interface KegiatanDao {


    @Insert
    suspend fun insert(kegiatan: KegiatanEntity): Long

    @Query("SELECT * FROM kegiatan")
    suspend fun getAll(): List<KegiatanEntity>

    @Delete
    suspend fun delete(kegiatan: KegiatanEntity)
}