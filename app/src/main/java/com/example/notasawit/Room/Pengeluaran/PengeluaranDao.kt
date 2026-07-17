package com.example.notasawit.Room.Pengeluaran

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.notasawit.Room.Produksi.ProduksiEntity

@Dao
interface PengeluaranDao {

    @Insert
    suspend fun insert(data: PengeluaranEntity): Long

    @Query("SELECT * FROM biaya_operasional")
    suspend fun getAll(): List<PengeluaranEntity>

    @Query("""
        SELECT * FROM biaya_operasional
        WHERE isSynced = 0
    """)
    suspend fun getUnsynced(): List<PengeluaranEntity>

    @Query("""
        UPDATE biaya_operasional
        SET isSynced = 1
        WHERE localId = :id
    """)
    suspend fun updateSynced(id: Int)

    @Query("""
    DELETE FROM biaya_operasional
    WHERE localId = :id
    """)
    suspend fun deleteById(id: Int)
}