package com.example.notasawit.Room.DetailKegiatan


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface DetailKegiatanDao {

    @Insert
    suspend fun insert(detail: DetailKegiatanEntity)

    @Query("""
        SELECT * FROM detail_kegiatan
        WHERE kegiatanId = :kegiatanId
    """)
    suspend fun getByKegiatan(
        kegiatanId: Int
    ): List<DetailKegiatanEntity>

    @Query("""
    DELETE FROM detail_kegiatan
    WHERE kegiatanId = :kegiatanId
    """)
    suspend fun deleteByKegiatan(kegiatanId: Int)

}