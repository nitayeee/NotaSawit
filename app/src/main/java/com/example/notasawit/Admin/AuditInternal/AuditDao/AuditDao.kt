package com.example.notasawit.Admin.AuditInternal.AuditDao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notasawit.Room.AuditEntity.AuditForm

@Dao
interface AuditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audit: AuditForm)

    @Query("SELECT * FROM audit_table")
    suspend fun getAllAudit(): List<AuditForm>

    // Perintah untuk mengambil semua data audit yang BELUM sinkron ke Laravel
    @Query("SELECT * FROM audit_table WHERE isSynced = 0")
    suspend fun getUnsyncedAudit(): List<AuditForm>

    // Perintah untuk mengubah status menjadi "Sudah Sinkron" setelah sukses dikirim ke Laravel
    @Query("UPDATE audit_table SET isSynced = 1 WHERE idAudit = :id")
    suspend fun markAsSynced(id: String)

}