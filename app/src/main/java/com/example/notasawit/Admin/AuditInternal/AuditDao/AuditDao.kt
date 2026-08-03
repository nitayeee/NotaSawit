package com.example.notasawit.Admin.AuditInternal.AuditDao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.notasawit.Room.AuditEntity.AuditAnswer
import com.example.notasawit.Room.AuditEntity.AuditHeader

@Dao
interface AuditDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHeader(auditHeader: AuditHeader)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnswers(auditAnswers: List<AuditAnswer>)

    @Query("SELECT * FROM audit_header")
    suspend fun getAllAuditHeaders(): List<AuditHeader>

    @Query("SELECT * FROM audit_answer WHERE idAudit = :idAudit")
    suspend fun getAnswersForAudit(idAudit: String): List<AuditAnswer>

    // Perintah untuk mengambil semua data audit yang BELUM sinkron ke Laravel
    @Query("SELECT * FROM audit_header WHERE isSynced = 0")
    suspend fun getUnsyncedAudit(): List<AuditHeader>

    // Perintah untuk mengubah status menjadi "Sudah Sinkron" setelah sukses dikirim ke Laravel
    @Query("UPDATE audit_header SET isSynced = 1 WHERE idAudit = :id")
    suspend fun markAsSynced(id: String)
    
    // Cek audit terakhir petani pada periode tertentu
    @Query("SELECT * FROM audit_header WHERE namaPetani = :namaPetani AND periode = :periode ORDER BY auditAttempt DESC LIMIT 1")
    suspend fun getLastAuditForPetani(namaPetani: String, periode: String): AuditHeader?

}