package com.example.notasawit.Room.Produksi

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.notasawit.Room.KegiatanPetani.KegiatanEntity

@Dao
interface ProduksiDao {

    @Insert
    suspend fun insert(data: ProduksiEntity): Long

    @Query("SELECT * FROM produksi")
    suspend fun getAll(): List<ProduksiEntity>

    @Query("""
        SELECT * FROM produksi
        WHERE isSynced = 0
    """)
    suspend fun getUnsynced(): List<ProduksiEntity>

    @Query("""
        UPDATE produksi
        SET isSynced = 1
        WHERE localId = :id
    """)
    suspend fun updateSynced(id: Int)

    @Query("""
    DELETE FROM produksi
    WHERE localId = :id
    """)
    suspend fun deleteById(id: Int)
}