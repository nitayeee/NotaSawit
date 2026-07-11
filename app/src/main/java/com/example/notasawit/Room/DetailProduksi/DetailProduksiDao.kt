package com.example.notasawit.Room.DetailProduksi

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.notasawit.Room.DetailKegiatan.DetailKegiatanEntity

@Dao
interface DetailProduksiDao {

    @Insert
    suspend fun insert(detail: DetailProduksiEntity)

    @Query("""
        SELECT * FROM detail_produksi
        WHERE produksiId = :produksiId
    """)
    suspend fun gethByProduksi(
        produksiId: Int
    ): List<DetailProduksiEntity>

    @Query("""
    DELETE FROM detail_produksi
    WHERE produksiId = :produksiId
    """)
    suspend fun deleteByKegiatan(produksiId: Int)

}