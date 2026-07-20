package com.example.notasawit.Room.KunjunganLahanEntity

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface KunjunganLahanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKunjunganLahan(kunjunganLahan: KunjunganLahanForm)

    @Update
    suspend fun updateKunjunganLahan(kunjunganLahan: KunjunganLahanForm)

    @Query("SELECT * FROM kunjungan_lahan_table")
    suspend fun getAllKunjunganLahan(): List<KunjunganLahanForm>

    @Query("SELECT * FROM kunjungan_lahan_table WHERE idKunjungan = :id")
    suspend fun getKunjunganLahanById(id: String): KunjunganLahanForm?
    
    @Query("SELECT * FROM kunjungan_lahan_table WHERE isSynced = 0")
    suspend fun getUnsyncedKunjunganLahan(): List<KunjunganLahanForm>
    
    @Query("UPDATE kunjungan_lahan_table SET isSynced = 1 WHERE idKunjungan = :id")
    suspend fun markAsSynced(id: String)
}
