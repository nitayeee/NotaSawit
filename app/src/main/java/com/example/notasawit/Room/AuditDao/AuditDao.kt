package com.example.notasawit.Room.AuditDao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notasawit.Room.AuditEntity.AuditForm

@Dao
interface AuditDao {
    // 1. Perintah untuk menyimpan atau mengupdate data audit di HP
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAudit(audit: AuditForm)

    // 2. Perintah untuk mengambil semua data audit yang BELUM sinkron ke Laravel
    @Query("SELECT * FROM audit_table WHERE isSynced = 0")
    suspend fun getUnsyncedAudit(): List<AuditForm>

    // 3. Perintah untuk mengubah status menjadi "Sudah Sinkron" setelah sukses dikirim ke Laravel
    @Query("UPDATE audit_table SET isSynced = 1 WHERE idAudit = :id")
    suspend fun markAsSynced(id: String)
}