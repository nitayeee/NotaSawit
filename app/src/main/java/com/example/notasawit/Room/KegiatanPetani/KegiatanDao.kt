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

    @Query("""
        SELECT * FROM kegiatan
        WHERE isSynced = 0
    """)
    suspend fun getUnsynced(): List<KegiatanEntity>

    @Query("""
        UPDATE kegiatan
        SET isSynced = 1
        WHERE localId = :id
    """)
    suspend fun updateSynced(id: Int)

    @Query("""
    DELETE FROM kegiatan
    WHERE localId = :id
    """)
    suspend fun deleteById(id: Int)

}