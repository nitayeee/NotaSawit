package com.example.notasawit.Room.DetailPengeluaran

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.notasawit.Room.DetailProduksi.DetailProduksiEntity

@Dao
interface DetailPengeluaranDao {

    @Insert
    suspend fun insert(detail: DetailPengeluaranEntity)

    @Query("""
        SELECT * FROM detail_pengeluaran
        WHERE biaya_operasional_id = :biaya_operasional_id
    """)
    suspend fun getByPengeluaran(
        biaya_operasional_id: Int
    ): List<DetailPengeluaranEntity>

    @Query("""
    DELETE FROM detail_pengeluaran
    WHERE biaya_operasional_id = :biaya_operasional_id
    """)
    suspend fun deleteByPengeluaran(biaya_operasional_id: Int)

}