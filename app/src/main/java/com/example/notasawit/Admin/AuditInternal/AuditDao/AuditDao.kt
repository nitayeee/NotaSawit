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

}